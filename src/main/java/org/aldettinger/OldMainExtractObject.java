package org.aldettinger;

import java.time.LocalDate;

import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class OldMainExtractObject {

    static class Extract {
        private String customerName;
        private LocalDate customerBirthday;
        private boolean customerSatisfied;
    }

    interface Extractor {
        //@UserMessage("Extract information about a person from the text delimited by triple backticks ```{{text}}```")
        @UserMessage("Extract information from the text delimited by triple backticks: ```{{text}}```")
        Extract extractFromText(@V("text") String text);
    }

    public static void main(String[] args) {

        LocalAiChatModel model = LocalAiChatModel.builder()
                .baseUrl("http://localhost:8080")
                .logRequests(true)
                .logResponses(true)
                .modelName("gpt-4")
                .temperature(0.0)
                .build();

        Extractor extractor = AiServices.create(Extractor.class, model);

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
                "Customer: Hello, I'm John. I need to share a problem with you. Actually, the insurance has reimbursed only half the money have spent due to the accident." +
                "Operator: Hello John, could you please give me your last name so that I can find you contract." +
                "Customer: Sure, my surname is Doe." +
                "Operator: And last thing, I need to know the date you were born." +
                "Customer: Yes, so I was born in 1986, actually during the first day of November." +
                "Operator: Great, I see your contract now. Actually, your partner recently cancelled the full reimbursement option. This explain the half reimbursement." +
                "Customer: Ah damn, I knew something wrong happened. We discussed this together and then she cancelled it against my will." +
                "Operator: Oh, I'm sorry to hear that." +
                "Customer: Ok, many thanks for information anyway. Bye." +
                "Operator: Sure, bye.";
        
        String text2 = "Operator: Bonjour, bienvenue chez Star assurances. Comment puis-je vous aider ?" +
                "Customer: Bonjour, je suis Jean-Marc Martin et j'appelle pour obtenir un certificat d'assurance." +
                "Operator: D'accord Mr Martin, puis-je vous demander votre date de naissance s'il vous plait ?" +
                "Customer: Tout à fait, vous pouvez m'appeller Jean-Marc si vous le souhaitez. Je suis venu au monde le 23 février 1976." +
                "Operator: Très bien, je suis vraiment désolé mais je vois sur votre dossier que vous n'êtes plus assuré chez nous." +
                "Customer: Ah très bien, je me rappelle maintenant que nous avons changer d'assurance." +
                "Operator: Vous m'en voyez fort désolé. Puis-je faire quelque chose pour vous inviter à revenir chez nous." +
                "Customer: Non merci, j'étais tout à fait satisfait. En fait, nous avons du changer d'assurance par obligation légale suite à un changement de profession." +
                "Operator: Je vois, cela me désole un peu mais j'ai bien l'impression que je ne puis rien faire pour vous." +
                "Customer: Si si, vous m'avez déjà beaucoup aidé. Merci d'avoir pris du temps pour me répondre." +
                "Operator: D'accord, bonne journée à vous.";

        // The flow could be, if anyone is hurt, then send a copy to ambulance (nats ?)
        // If the tow truck is needed, 

        Extract extract = extractor.extractFromText(text);
        System.out.println(extract.customerName);
        System.out.println(extract.customerBirthday);
        System.out.println(extract.customerSatisfied);
    }
}
//   curl http://localhost:8080/v1/chat/completions -H "Content-Type:application/json" -d '{ "model": "gpt-4", "messages": [{"role": "user", "content": "How are you doing?", "temperature": 0.1}] }'  

/*
{
"firstName": "John",
"lastName": "Doe",
"birthDate": "1968-12-31",
"this$0": {
}
}

Explanation:
- The firstName is "John".
- The lastName is "Doe".
- The birthDate is "1968-12-31".
- The this$0 is an empty object, as it is not specified in the given information.

Note: The JSON format is used to represent structured data in a human-readable way. In this case, it is used to store information about a person, including their first and last name, birth date, and an empty object for the unspecified information.
*/
