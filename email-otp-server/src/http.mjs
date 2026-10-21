import { createServer } from 'node:http';
import { OtpError } from './otp-service.mjs';

export function createOtpServer({ service, auth, projectId }) {
  return createServer(async (req, res) => {
    const reply = (status, data) => {
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
      res.end(JSON.stringify(data));
    };
    if (req.method === 'GET' && req.url === '/health') return reply(200, { ok: true, projectId });
    if (req.method !== 'POST' || !['/email-otp/send', '/email-otp/verify'].includes(req.url))
      return reply(404, { code: 'NOT_FOUND', message: 'Không tìm thấy chức năng.' });
    try {
      if (!req.headers['content-type']?.startsWith('application/json'))
        throw new OtpError(415, 'INVALID_BODY', 'Yêu cầu phải có định dạng JSON.');
      const bearer = /^Bearer ([^\s]+)$/.exec(req.headers.authorization || '');
      if (!bearer) throw new OtpError(401, 'UNAUTHENTICATED', 'Vui lòng đăng nhập lại để xác thực email.');
      let token, user;
      try { token = await auth.verifyIdToken(bearer[1], true); user = await auth.getUser(token.uid); }
      catch { throw new OtpError(401, 'UNAUTHENTICATED', 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.'); }
      let text = '';
      for await (const chunk of req) {
        text += chunk.toString('utf8');
        if (Buffer.byteLength(text) > 2048) throw new OtpError(413, 'INVALID_BODY', 'Yêu cầu quá lớn.');
      }
      let body;
      try { body = JSON.parse(text || '{}'); } catch { throw new OtpError(400, 'INVALID_BODY', 'Dữ liệu không hợp lệ.'); }
      if (!body || typeof body !== 'object' || Array.isArray(body)) throw new OtpError(400, 'INVALID_BODY', 'Dữ liệu không hợp lệ.');
      const result = req.url.endsWith('/send')
        ? await service.send(user, req.socket.remoteAddress || 'unknown')
        : await service.verify(user, body.challengeId, body.code);
      reply(200, result);
    } catch (error) {
      if (error instanceof OtpError) reply(error.status, { code: error.code, message: error.message, ...error.extra });
      else { console.error('OTP request failed:', error.code || error.name || 'UNKNOWN'); reply(503, { code: 'SERVER_UNAVAILABLE', message: 'Dịch vụ xác thực tạm thời không khả dụng.' }); }
    }
  });
}
