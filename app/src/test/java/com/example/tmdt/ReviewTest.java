package com.example.tmdt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** UC26: quy tắc thuần Java của đánh giá hai chiều. */
public class ReviewTest {
    @Test public void reviewIdIsOnePerAuthorPerContract() {
        assertEquals("c1__u1", Review.idFor("c1", "u1"));
    }
    @Test public void ratingLabels() {
        assertEquals("Rất hài lòng", Review.ratingLabel(5));
        assertEquals("Rất không hài lòng", Review.ratingLabel(1));
        assertEquals("Chọn số sao", Review.ratingLabel(0));
    }
}
