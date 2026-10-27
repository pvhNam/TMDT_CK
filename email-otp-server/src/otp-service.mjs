import { createHmac, randomBytes, randomInt, timingSafeEqual } from 'node:crypto';

export class OtpError extends Error {
  constructor(status, code, message, extra = {}) {
    super(message); Object.assign(this, { status, code, extra });
  }
}
const deny = (status, code, message, extra) => new OtpError(status, code, message, extra);
const equal = (a, b) => typeof a === 'string' && typeof b === 'string'
  && a.length === b.length && timingSafeEqual(Buffer.from(a), Buffer.from(b));

/** All code checks, attempt counters and reservations live on the server. */
export class OtpService {
  constructor({ store, auth, mail, secret, now = Date.now, dailyLimit = 100 }) {
    if (!secret || secret.length < 32) throw new Error('OTP_HMAC_SECRET must contain at least 32 characters.');
    Object.assign(this, { store, auth, mail, secret, now, dailyLimit });
  }
  digest(value) { return createHmac('sha256', this.secret).update(value).digest('hex'); }
  challengeKey(uid) { return `_email_otp/${this.digest(uid)}`; }
  codeHash(user, challenge, code) { return this.digest(`${user.uid}\n${user.email}\n${challenge}\n${code}`); }
  validUser(user) {
    if (!user || user.disabled || !user.email) throw deny(403, 'INVALID_ACCOUNT', 'Tài khoản không thể xác thực email.');
  }
  async send(user, ip) {
    this.validUser(user);
    if (user.emailVerified) return { verified: true };
    const now = this.now(), id = randomBytes(24).toString('hex');
    const code = String(randomInt(0, 1_000_000)).padStart(6, '0');
    const key = this.challengeKey(user.uid), ipKey = `_email_otp_limits/${this.digest(ip)}`;
    const dailyKey = `_email_otp_limits/day-${Math.floor(now / 86400000)}`;
    await this.store.transaction(async tx => {
      const previous = await tx.get(key), bucket = await tx.get(ipKey), daily = await tx.get(dailyKey);
      if (previous && previous.nextSendAt > now) throw deny(429, 'RESEND_WAIT', 'Vui lòng chờ trước khi gửi lại mã.', {
        retryAfter: Math.ceil((previous.nextSendAt - now) / 1000), challengeId: previous.id,
        expiresIn: Math.max(0, Math.ceil((previous.expiresAt - now) / 1000))
      });
      if (previous?.status === 'VERIFYING' && previous.leaseUntil > now)
        throw deny(409, 'VERIFYING', 'Mã đang được xử lý. Vui lòng thử lại sau.');
      const emailCount = previous && previous.windowStart + 3600000 > now ? previous.sentCount : 0;
      const ipCount = bucket && bucket.windowStart + 3600000 > now ? bucket.count : 0;
      if (emailCount >= 5 || ipCount >= 20 || (daily?.count || 0) >= this.dailyLimit)
        throw deny(429, 'SEND_LIMIT', 'Đã đạt giới hạn gửi mã. Vui lòng thử lại sau.');
      tx.set(key, {
        id, email: user.email, codeHash: this.codeHash(user, id, code), attempts: 0,
        status: 'READY', expiresAt: now + 300000, nextSendAt: now + 60000,
        windowStart: emailCount ? previous.windowStart : now, sentCount: emailCount + 1
      });
      tx.set(ipKey, { windowStart: ipCount ? bucket.windowStart : now, count: ipCount + 1 });
      tx.set(dailyKey, { count: (daily?.count || 0) + 1 });
    });
    try {
      await this.mail.send({ to: user.email, code });
    } catch {
      // Invalidate this code without overwriting a newer challenge.
      await this.store.transaction(async tx => {
        const active = await tx.get(key);
        if (active?.id === id) tx.set(key, { ...active, status: 'MAIL_FAILED', codeHash: '', nextSendAt: now });
      });
      throw deny(503, 'MAIL_UNAVAILABLE', 'Chưa gửi được email. Vui lòng thử lại sau.');
    }
    return { challengeId: id, expiresIn: 300, retryAfter: 60 };
  }
  async verify(user, challengeId, code) {
    this.validUser(user);
    if (user.emailVerified) return { verified: true };
    if (typeof challengeId !== 'string' || !/^[a-f0-9]{48}$/.test(challengeId)
      || typeof code !== 'string' || !/^[0-9]{6}$/.test(code))
      throw deny(400, 'INVALID_CODE', 'Vui lòng nhập mã OTP gồm 6 chữ số.');
    const key = this.challengeKey(user.uid), now = this.now(), claim = randomBytes(16).toString('hex');
    const failure = await this.store.transaction(async tx => {
      const active = await tx.get(key);
      if (!active || active.id !== challengeId || active.email !== user.email)
        return deny(400, 'CODE_REPLACED', 'Mã không còn hiệu lực. Hãy dùng mã mới nhất hoặc gửi lại mã.');
      if (active.expiresAt <= now) return deny(410, 'CODE_EXPIRED', 'Mã OTP đã hết hạn. Vui lòng gửi lại mã.');
      if (active.status === 'USED') return deny(409, 'CODE_USED', 'Mã OTP đã được sử dụng.');
      if (active.status === 'MAIL_FAILED') return deny(400, 'CODE_REPLACED', 'Email chưa gửi thành công. Vui lòng gửi lại mã.');
      if (active.attempts >= 5) return deny(429, 'ATTEMPTS_EXCEEDED', 'Bạn đã nhập sai 5 lần. Vui lòng gửi lại mã.');
      if (active.status === 'VERIFYING' && active.leaseUntil > now)
        return deny(409, 'VERIFYING', 'Mã đang được xử lý. Vui lòng thử lại sau.');
      if (!equal(active.codeHash, this.codeHash(user, challengeId, code))) {
        tx.set(key, { ...active, attempts: active.attempts + 1 });
        return deny(400, 'CODE_INCORRECT', `Mã OTP chưa đúng. Còn ${4 - active.attempts} lần thử.`);
      }
      tx.set(key, { ...active, status: 'VERIFYING', claim, leaseUntil: now + 30000 });
      return null;
    });
    if (failure) throw failure;
    try {
      // Fetch again so changing an account email invalidates its old code.
      const fresh = await this.auth.getUser(user.uid);
      if (fresh.email !== user.email || fresh.disabled) throw new Error('Account changed');
      await this.auth.updateUser(user.uid, { email: user.email, emailVerified: true });
    } catch {
      await this.store.transaction(async tx => {
        const active = await tx.get(key);
        if (active?.claim === claim) tx.set(key, { ...active, status: 'READY', claim: '', leaseUntil: 0 });
      });
      throw deny(503, 'VERIFY_UNAVAILABLE', 'Chưa cập nhật được xác thực. Vui lòng thử lại.');
    }
    await this.store.transaction(async tx => {
      const active = await tx.get(key);
      if (active?.claim === claim) tx.set(key, { ...active, status: 'USED', codeHash: '', claim: '', leaseUntil: 0 });
    });
    return { verified: true };
  }
}
