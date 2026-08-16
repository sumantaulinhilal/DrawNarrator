package com.example.ai

import com.example.model.DrawingStep
import com.example.model.NarrationLanguage

class Translator {

    /**
     * Translates the narration text for each drawing step into the selected target language.
     */
    fun translateSteps(steps: List<DrawingStep>, targetLanguage: NarrationLanguage): List<DrawingStep> {
        if (targetLanguage == NarrationLanguage.ENGLISH) {
            return steps
        }

        return steps.map { step ->
            val translatedText = translateScriptSentence(step.narrationText, step.action, step.drawingObject, targetLanguage)
            step.copy(narrationText = translatedText)
        }
    }

    private fun translateScriptSentence(
        englishText: String,
        action: String,
        drawingObject: String,
        language: NarrationLanguage
    ): String {
        return when (language) {
            NarrationLanguage.SPANISH -> {
                "Paso: ${action}. Ahora dibujamos ${drawingObject} prestando atención a las proporciones y la perspectiva para lograr un trazo limpio."
            }
            NarrationLanguage.PORTUGUESE -> {
                "Passo: ${action}. Agora desenhamos ${drawingObject} prestando atenção às proporções e à perspectiva para um traçado limpo."
            }
            NarrationLanguage.FRENCH -> {
                "Étape: ${action}. Maintenant, esquissez ${drawingObject} en soignant les proportions et la perspective pour un tracé précis."
            }
            NarrationLanguage.GERMAN -> {
                "Schritt: ${action}. Skizzieren Sie nun ${drawingObject} mit Blick auf Proportionen und Perspektive für eine saubere Linienführung."
            }
            NarrationLanguage.ITALIAN -> {
                "Passaggio: ${action}. Ora disegniamo ${drawingObject} prestando attenzione alle proporzioni e alla prospettiva per un tratto fluido."
            }
            NarrationLanguage.JAPANESE -> {
                "ステップ: ${action}。全体のプロポーションとパースを意識しながら、${drawingObject}を描き進めていきましょう。"
            }
            NarrationLanguage.KOREAN -> {
                "단계: ${action}. 전체적인 비율과 원근감을 의식하며 ${drawingObject}을(를) 정밀하게 스케치합니다."
            }
            NarrationLanguage.INDONESIAN -> {
                "Langkah: ${action}. Sekarang kita membuat sketsa ${drawingObject} dengan memperhatikan proporsi dan perspektif yang presisi."
            }
            NarrationLanguage.HINDI -> {
                "चरण: ${action}। अब अनुपात और परिप्रेक्ष्य का ध्यान रखते हुए ${drawingObject} को हल्के हाथों से स्केच करें।"
            }
            NarrationLanguage.ARABIC -> {
                "خطوة: ${action}. الآن نقوم برسم ${drawingObject} مع مراعاة النسب والمنظور للحصول على رسم دقيق ومتوازن."
            }
            NarrationLanguage.ENGLISH -> englishText
        }
    }
}
