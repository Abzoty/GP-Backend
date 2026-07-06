package com.gp.GP_backend.domain.questionnaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public
class QuestionnaireAnswersRequest {
    
    public Map<Integer, String> answers;
}
