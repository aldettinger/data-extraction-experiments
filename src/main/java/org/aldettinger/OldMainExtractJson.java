package org.aldettinger;

import java.time.Duration;
import java.time.LocalDate;

import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class OldMainExtractJson {

    /*
    static class Extract {
        private String customerName;
        private LocalDate customerBirthday;
        private boolean customerSatisfied;
    }*/

    interface CamelExtractor {
        @UserMessage(
                // TODO remove \n, more concise, and we can pretty print in next steps
                // TODO here we are sending text, which could match camel body, but the source of text could as well be a header
                "Extract information from the text delimited by triple backticks: ```{{text}}```."
                //+ "customerBirthday should be a date with following format YYYY-MM-DD."
                + "The output should be formatted into JSON as per the following schema."
                +"{\n"
                + " camel:\n"
                + " {\n"
                + "    registry:\n"
                + "    {\n"
                + "        'customerSatisfied': (type: boolean)\n"
                + "    }\n"
                + "    exchange:\n"
                + "    {\n"
                + "        property:\n"
                + "        {\n"
                + "            'customerName': (type: string)\n"
                + "        }\n"
                + "        headers:\n"
                + "        {\n"
                + "            'customerBirthday': (type: date string)\n"
                + "        }\n"
                + "        'body': (type: string)\n"
                + "    }\n"
                + " }\n"
                + "}"
                )
        String extractFromText(@V("text") String text);
    }

    /**
     * On last attempt, it was working with text
     * I then experimented with text1, but then local-ai seems to go into infinite loops...
     */
    public static void main(String[] args) {

        LocalAiChatModel model = LocalAiChatModel.builder()
                .baseUrl("http://localhost:8080")
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(1))
                .maxRetries(0) // Bind with camel retry whatever ? Make any sense ?
                .modelName("gpt-4")
                .temperature(0.0)
                .build();

        CamelExtractor extractor = AiServices.create(CamelExtractor.class, model);

        String text = "Operator: Hello, how may I help you ?" +
                      "Customer: Hello, I'm calling because I need to declare an accident on my main vehicle." +
                      "Operator: Ok, can you please give me your name ?" +
                      "Customer: My name is Sarah London." +
                      "Operator: Could you please give me your birth date ?" +
                      "Customer: 1986, July the 10th" +
                      "Operator: Ok, I've got your contract and I'm happy to share with you that we'll be able to reimburse all spent linked to this accident"
                      +
                      "Customer: Oh great, many thanks";
        
        String text1 = "Operator: Hello, how may I help you ?" +
                "Customer: Hello, I'm John. I need to share a problem with you. Actually, the insurance has reimbursed only half the money I have spent due to the accident." +
                "Operator: Hello John, could you please give me your last name so that I can find you contract." +
                "Customer: Sure, my surname is Doe." +
                "Operator: And last thing, I need to know the date you were born." +
                "Customer: Yes, so I was born in 1986, actually during the first day of November." +
                "Operator: Great, I see your contract now. Actually, your partner recently cancelled the full reimbursement option. This explain the half reimbursement." +
                "Customer: Ah damn, I knew something wrong happened. We discussed this together and then she cancelled it against my will." +
                "Operator: Oh, I'm sorry to hear that." +
                "Customer: Ok, many thanks for information anyway. Bye." +
                "Operator: Sure, bye.";

        // The flow could be, if anyone is hurt, then send a copy to ambulance (nats ?)
        // If the tow truck is needed, 

        String answer = extractor.extractFromText(text1);
        System.out.println(answer);
    }
}