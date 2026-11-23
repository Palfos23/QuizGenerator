package com.quizapp.dto;

import com.quizapp.model.DailyQuizAnswerVerdict;

/** One written answer, as the admin's answer review needs it - just the columns, not the whole answer/attempt/question objects. */
public record DailyQuizReviewRow(Long setId, Long questionId, String answerText, DailyQuizAnswerVerdict verdict) {}
