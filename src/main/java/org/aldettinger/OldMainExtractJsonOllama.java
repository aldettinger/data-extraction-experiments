package org.aldettinger;

import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * How to run the demo: In a terminal, run:
 * MODEL_NAME=llama2
 * docker run -p 11434:11434 langchain4j/ollama-${MODEL_NAME}:latest
 * 
 * Then run method main.
 * 
 */
public class OldMainExtractJsonOllama {

    static final String MODEL_NAME = "codellama"; // try "orca-mini", "mistral", "llama2", "llama3", "codellama", "phi" or "tinyllama"
    static final String LANGCHAIN4J_OLLAMA_IMAGE_NAME = "langchain4j/ollama-" + MODEL_NAME + ":latest";

    /**
     * - co-reference not resolved
     * - JSON schema not respected
     * - customer satisfaction detection is very sensible to prompt change
     */
    static final String LLAMA2_PROMPT =
            "Extract information from the text delimited by triple backticks: ```{{text}}```."
            + "The output should be formatted into JSON, strictly conforming to the JSON schema delimited by triple dollars."
            + "$$${"
            + " camel:{"
            + "  registry: {"
            + "   customerSatisfied: (type: boolean)"
            + "  },"
            + "  exchange: {"
            + "   property: {"
            + "    customerName: (type: string)"
            + "   },"
            + "   headers: {"
            + "    'customerBirthday': (type: date string)"
            + "   },"
            + "   body: (type: string)"
            + "  }"
            + " }"
            + "}$$$."
            + "The body value in the JSON should be a 20 words summary of the text delimited by triple backticks."
            + "Only fields appearing in the JSON schema should be output. Do not create extra field."
            + "The customerBirthday field should be formatted as DD-MM-YYYY.";

    /**
     * + co-reference resolved
     * - JSON schema not respected
     * + customer satisfaction detection seems good
     */
    static final String MISTRAL_PROMPT =
            "Extract information from the text delimited by triple backticks: ```{{text}}```."
            + "The output should be formatted into JSON, strictly conforming to the JSON schema delimited by triple dollars."
            + "$$${"
            + " camel:{"
            + "  registry: {"
            + "   customerSatisfied: (type: boolean)"
            + "  },"
            + "  exchange: {"
            + "   property: {"
            + "    customerName: (type: string)"
            + "   },"
            + "   headers: {"
            + "    'customerBirthday': (type: date string)"
            + "   },"
            + "   body: (type: string)"
            + "  }"
            + " }"
            + "}$$$."
            + "The body value in the JSON should be a 20 words summary of the text delimited by triple backticks."
            + "Only fields appearing in the JSON schema should be output. Do not create extra field."
            + "The customerBirthday field should be formatted as DD-MM-YYYY.";

    /**
     * + co-reference resolved
     * + JSON schema fully respected
     * + customer satisfaction detection seems good
     *
     * - The text summary is quite accurate.
     */
    static final String CODE_LLAMA_PROMPT =
            "Extract information from the text delimited by triple backticks: ```{{text}}```."
            + "The output should be formatted into JSON, strictly conforming to the JSON schema delimited by triple dollars."
            + "$$${"
            + " camel:{"
            + "  registry: {"
            + "   customerSatisfied: (type: boolean)"
            + "  },"
            + "  exchange: {"
            + "   property: {"
            + "    customerName: (type: string)"
            + "   },"
            + "   headers: {"
            + "    'customerBirthday': (type: date string)"
            + "   },"
            + "   body: (type: string)"
            + "  }"
            + " }"
            + "}$$$."
            //+ "The body field should be a 10 words summary of the text delimited by triple backticks."
            + "The body field should be a very concise summary of the text delimited by triple backticks, just a few words."
            + "The customerBirthday field should be formatted as DD-MM-YYYY."
            + "Only fields appearing in the JSON schema should be output. Do not create extra field.";
    
    interface CamelExtractor {

        @UserMessage(CODE_LLAMA_PROMPT)
        // As we are talking about fields for customerName and customerBirthday it stores them at the same level, that does not match the JSON schema
        String extractFromText(@V("text") String text);
        
        /*
         * Apparently, the customerBirthday format specification from the prompt really has an impact.
         */
    }

    public static void main(String[] args) {

        String url = String.format("http://%s:%d", "localhost", 11434);

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(url)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .format("json")
                .timeout(Duration.ofMinutes(1L))
                .build();

        CamelExtractor extractor = AiServices.create(CamelExtractor.class, model);

        String[] texts = {
                "Operator: Hello, how may I help you ?" +
                           "Customer: Hello, I'm calling because I need to declare an accident on my main vehicle." +
                           "Operator: Ok, can you please give me your name ?" +
                           "Customer: My name is Sarah London." +
                           "Operator: Could you please give me your birth date ?" +
                           "Customer: 1986, July the 10th." +
                           "Operator: Ok, I've got your contract and I'm happy to share with you that we'll be able to reimburse all spent linked to this accident."
                           +
                           "Customer: Oh great, many thanks.",

                "Operator: Hello, how may I help you ?" +
                                                              "Customer: Hello, I'm John. I need to share a problem with you. Actually, the insurance has reimbursed only half the money I have spent due to the accident."
                                                              +
                                                              "Operator: Hello John, could you please give me your last name so that I can find you contract."
                                                              +
                                                              "Customer: Sure, my surname is Doe." +
                                                              "Operator: And last thing, I need to know the date you were born."
                                                              +
                                                              "Customer: Yes, so I was born in 2001, actually during the first day of November."
                                                              +
                                                              "Operator: Great, I see your contract now. Actually, the full reimbursement option has been cancelled automatically by our system. This explain the half reimbursement."
                                                              +
                                                              "Customer: Ah damn, this is not acceptable. I've not even been notified about this automatic change."
                                                              +
                                                              "Operator: Oh, I'm sorry to hear that but the full reimbursement option was free for one year and at the time of subsription you were not interested in automatic renewal."
                                                              +
                                                              "Customer: I don't discuss that. The important fact is that I should have been notified."
                                                              +
                                                              "Operator: Sure, I understand your resentment. The best I can do is to inform my manager."
                                                              +
                                                              "Customer: Ok, let's do that. Good bye." +
                                                              "Operator: Good bye. And again let me apologize for the issue." };

        for (String text : texts) {
            String answer = extractor.extractFromText(text);
            System.out.println(toPrettyFormat(answer));
            System.out.println("---------------------------------------------------------");
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
