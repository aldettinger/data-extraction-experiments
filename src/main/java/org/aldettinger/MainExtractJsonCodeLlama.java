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

public class MainExtractJsonCodeLlama {

    static final String MODEL_NAME = "codellama"; // Other values could be "orca-mini", "mistral", "llama2", "llama3", "codellama", "phi" or "tinyllama"
    static final String LANGCHAIN4J_OLLAMA_IMAGE_NAME = "langchain4j/ollama-" + MODEL_NAME + ":latest";

    static final String CODE_LLAMA_PROMPT = "Extract information from the text delimited by triple backticks: ```{{text}}```."
                                            + "The output should be formatted into JSON, strictly conforming to the JSON schema delimited by triple dollars."
                                            + "$$${"
                                            + " conversation:{"
                                            + "  emotions: {"
                                            + "   customerSatisfied: (type: boolean)"
                                            + "  },"
                                            + "  entities: {"
                                            + "   id: {"
                                            + "    customerName: (type: string)"
                                            + "   },"
                                            + "   attributes: {"
                                            + "    customerBirthday: (type: date string)"
                                            + "   }"
                                            + "  },"
                                            + "  summary: (type: string)"
                                            + " }"
                                            + "}$$$."
                                            + "The summary field should be a very concise summary of the text delimited by triple backticks, just a few words."
                                            + "The customerBirthday field should be formatted as DD-MM-YYYY."
                                            + "Only fields appearing in the JSON schema should be output. Do not create extra field.";

    interface CamelExtractor {
        @UserMessage(CODE_LLAMA_PROMPT)
        String extractFromText(@V("text") String text);
    }

    public static void main(String[] args) throws IOException {

        String url = String.format("http://%s:%d", "localhost", 11434);

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(url)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .format("json")
                .timeout(Duration.ofMinutes(1L))
                .build();

        CamelExtractor extractor = AiServices.create(CamelExtractor.class, model);

        String[] resourceNames = {
                "01_sarah-london-10-07-1986-satisfied.txt", "02_john-doe-01-11-2001-unsatisfied.txt",
                "03_kate-boss-13-08-1999-satisfied.txt" };

        for (String resourceName : resourceNames) {
            String text = resourceToString(String.format("/texts/%s", resourceName), Charsets.UTF_8);

            long begin = System.currentTimeMillis();
            String answer = extractor.extractFromText(text);
            long duration = System.currentTimeMillis() - begin;

            System.out.println(toPrettyFormat(answer));
            System.out.println(String.format("----- Inference lasted %.1fs ------------------------------", duration / 1000.0));
        }
    }

    public static String toPrettyFormat(String jsonString) {
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String prettyJson = gson.toJson(json);

            return prettyJson;
        } catch (Exception ex) {
            return String.format("UNPARSEABLE JSON RETURNED BY MODEL:\n%s\n", jsonString);
        }
    }
}
