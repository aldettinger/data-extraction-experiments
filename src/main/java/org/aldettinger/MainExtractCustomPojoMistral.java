package org.aldettinger;

import static org.apache.commons.io.IOUtils.resourceToString;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import kotlin.text.Charsets;

public class MainExtractCustomPojoMistral {

    static final String MODEL_NAME = "mistral"; // Other values could be "orca-mini", "mistral", "llama2", "llama3", "codellama", "phi" or "tinyllama"
    static final String LANGCHAIN4J_OLLAMA_IMAGE_NAME = "langchain4j/ollama-" + MODEL_NAME + ":latest";

    /**
     * The customer birthday date format need to be forced to comply with what langchain4j gson parser need.
     */
    static final String CUSTOM_POJO_EXTRACT_PROMPT
            = "Extract information about a customer from the text delimited by triple backticks: ```{{text}}```."
              + "The customerBirthday field should be formatted as YYYY-MM-DD."
              + "The summary field should concisely relate the customer main ask.";

    static class CustomPojo {
        private boolean customerSatisfied;
        private String customerName;
        private LocalDate customerBirthday;
        private String summary;
    }

    interface CamelCustomPojoExtractor {
        @UserMessage(CUSTOM_POJO_EXTRACT_PROMPT)
        CustomPojo extractFromText(@V("text") String text);
    }

    public static void main(String[] args) throws IOException {

        String modelServingUrl = String.format("http://%s:%d", "localhost", 11434);

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(modelServingUrl)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .format("json")
                .timeout(Duration.ofMinutes(1L))
                .build();

        CamelCustomPojoExtractor extractor = AiServices.create(CamelCustomPojoExtractor.class, model);

        String[] conversationResourceNames = {
                "01_sarah-london-10-07-1986-satisfied.txt", "02_john-doe-01-11-2001-unsatisfied.txt",
                "03_kate-boss-13-08-1999-satisfied.txt" };

        for (String conversationResourceName : conversationResourceNames) {
            String conversation = resourceToString(String.format("/texts/%s", conversationResourceName), Charsets.UTF_8);

            long begin = System.currentTimeMillis();
            CustomPojo answer = extractor.extractFromText(conversation);
            long duration = System.currentTimeMillis() - begin;

            System.out.println(toPrettyFormat(answer));
            System.out.println(String.format("----- Inference lasted %.1fs ------------------------------", duration / 1000.0));
        }
    }

    private final static String FORMAT = "****************************************\n"
                                         + "customerSatisfied: %s\n"
                                         + "customerName: %s\n"
                                         + "customerBirthday: %td %tB %tY\n"
                                         + "summary: %s\n"
                                         + "****************************************\n";

    public static String toPrettyFormat(CustomPojo extract) {
        return String.format(FORMAT, extract.customerSatisfied, extract.customerName, extract.customerBirthday,
                extract.customerBirthday, extract.customerBirthday, extract.summary);
    }
}
