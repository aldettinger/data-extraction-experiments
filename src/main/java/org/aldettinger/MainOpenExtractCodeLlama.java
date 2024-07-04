package org.aldettinger;

import static org.apache.commons.io.IOUtils.resourceToString;

import java.io.IOException;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import kotlin.text.Charsets;

public class MainOpenExtractCodeLlama {

    static final String MODEL_NAME = "codellama"; // Other values could be "orca-mini", "mistral", "llama2", "llama3", "codellama", "phi" or "tinyllama"
    static final String LANGCHAIN4J_OLLAMA_IMAGE_NAME = "langchain4j/ollama-" + MODEL_NAME + ":latest";

    static final String OPEN_EXTRACT_PROMPT
            = "Extract information about an insurance related discussion from the text delimited by triple backticks: ```{{text}}```."
              +
              "The answer should be strictly formatted into JSON." +
              "The answer should only contain information quoted in the text delimited by triple backticks, do not invent information.";

    interface CamelOpenExtractor {
        @UserMessage(OPEN_EXTRACT_PROMPT)
        String extractFromText(@V("text") String text);
    }

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException {

        String modelServingUrl = String.format("http://%s:%d", "localhost", 11434);

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(modelServingUrl)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .format("json")
                .timeout(Duration.ofMinutes(1L))
                .build();

        CamelOpenExtractor extractor = AiServices.create(CamelOpenExtractor.class, model);

        String[] conversationResourceNames = {
                "01_sarah-london-10-07-1986-satisfied.txt", "02_john-doe-01-11-2001-unsatisfied.txt",
                "03_kate-boss-13-08-1999-satisfied.txt" };

        for (String conversationResourceName : conversationResourceNames) {
            String conversation = resourceToString(String.format("/texts/%s", conversationResourceName), Charsets.UTF_8);

            long begin = System.currentTimeMillis();
            String answer = extractor.extractFromText(conversation);
            long duration = System.currentTimeMillis() - begin;

            System.out.println(toPrettyFormat(answer));
            System.out.println(String.format("----- Inference lasted %.1fs ------------------------------", duration / 1000.0));
        }
    }

    public static String toPrettyFormat(String jsonString) {
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            return GSON.toJson(json);
        } catch (Exception ex) {
            return String.format("UNPARSEABLE JSON RETURNED BY MODEL:\n%s\n", jsonString);
        }
    }
}
