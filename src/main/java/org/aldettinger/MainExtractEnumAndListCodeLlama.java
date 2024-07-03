package org.aldettinger;

import static org.apache.commons.io.IOUtils.resourceToString;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import kotlin.text.Charsets;

public class MainExtractEnumAndListCodeLlama {

    static final String MODEL_NAME = "codellama"; // Other values could be "orca-mini", "mistral", "llama2", "llama3", "codellama", "phi" or "tinyllama"
    static final String LANGCHAIN4J_OLLAMA_IMAGE_NAME = "langchain4j/ollama-" + MODEL_NAME + ":latest";

    static final String ENUM_AND_LIST_EXTRACT_PROMPT
            = "Extract information about a customer from the text delimited by triple backticks: ```{{text}}```.";

    enum GENDER {
        MALE,
        FEMALE
    }

    static class EnumAndListPojo {
        private GENDER gender;
        private List<String> topics;
    }

    interface CamelEnumAndListExtractor {
        @UserMessage(ENUM_AND_LIST_EXTRACT_PROMPT)
        EnumAndListPojo extractFromText(@V("text") String text);
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

        CamelEnumAndListExtractor extractor = AiServices.create(CamelEnumAndListExtractor.class, model);

        String[] conversationResourceNames = {
                "01_sarah-london-10-07-1986-satisfied.txt", "02_john-doe-01-11-2001-unsatisfied.txt",
                "03_kate-boss-13-08-1999-satisfied.txt" };

        for (String conversationResourceName : conversationResourceNames) {
            String conversation = resourceToString(String.format("/texts/%s", conversationResourceName), Charsets.UTF_8);

            long begin = System.currentTimeMillis();
            EnumAndListPojo answer = extractor.extractFromText(conversation);
            long duration = System.currentTimeMillis() - begin;

            System.out.println(toPrettyFormat(answer));
            System.out.println(String.format("----- Inference lasted %.1fs ------------------------------", duration / 1000.0));
        }
    }

    private final static String FORMAT = "****************************************\n"
                                         + "gender: %s\n"
                                         + "topics: %s\n"
                                         + "****************************************\n";

    public static String toPrettyFormat(EnumAndListPojo extract) {
        return String.format(FORMAT, extract.gender, extract.topics);
    }
}
