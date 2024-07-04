package org.aldettinger;

import static org.apache.commons.io.IOUtils.resourceToString;

import java.io.IOException;
import java.time.Duration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import kotlin.text.Charsets;

public class MainOpenExtractResultCodeLlama {

    static final String MODEL_NAME = "codellama"; // Other values could be "orca-mini", "mistral", "llama2", "llama3", "codellama", "phi" or "tinyllama"
    static final String LANGCHAIN4J_OLLAMA_IMAGE_NAME = "langchain4j/ollama-" + MODEL_NAME + ":latest";

    static final String RESULT_EXTRACT_PROMPT
            = "Extract information about a customer from the text delimited by triple backticks: ```{{text}}```. The answer should be strictly formatted into JSON.";

    // TODO: Is there a langchain4j bug when one return a Result<CustomPojo> ???
    // ServiceOutputParser.typeOf: "} else if (((Class<?>) type).isEnum()) {"
    // ClassCastException ala
    // https://stackoverflow.com/questions/73982858/java-generics-reflection-get-classt-from-generic-returns-typevariableimpl-ins
    interface CamelResultExtractor {
        @UserMessage(RESULT_EXTRACT_PROMPT)
        Result<String> extractFromText(@V("text") String text);
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

        CamelResultExtractor extractor = AiServices.create(CamelResultExtractor.class, model);

        String[] conversationResourceNames = {
                "01_sarah-london-10-07-1986-satisfied.txt", "02_john-doe-01-11-2001-unsatisfied.txt",
                "03_kate-boss-13-08-1999-satisfied.txt" };

        for (String conversationResourceName : conversationResourceNames) {
            String conversation = resourceToString(String.format("/texts/%s", conversationResourceName), Charsets.UTF_8);

            long begin = System.currentTimeMillis();
            Result<String> answer = extractor.extractFromText(conversation);
            long duration = System.currentTimeMillis() - begin;

            System.out.println(toPrettyFormat(answer));
            System.out.println(String.format("----- Inference lasted %.1fs ------------------------------", duration / 1000.0));
        }
    }

    private final static String FORMAT = "****************************************\n"
                                         + "content: %s\n"
                                         + "sources: %s\n"
                                         + "tokenUsage: %s\n"
                                         + "****************************************\n";

    public static String toPrettyFormat(Result<String> extract) {
        return String.format(FORMAT, extract.content(), extract.sources(), extract.tokenUsage());
    }
}
