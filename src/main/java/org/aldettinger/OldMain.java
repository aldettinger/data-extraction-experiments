package org.aldettinger;

import java.time.LocalDate;

import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class OldMain {

    static class Person {
        private String firstName;
        private String lastName;
        private LocalDate birthDate;
    }

    interface PersonExtractor {
        //@UserMessage("Extract information about a person from the text delimited by triple backticks ```{{text}}```")
        @UserMessage("Extract information about a person from the text delimited by triple backticks. ```{{text}}```")
        Person extractPersonFrom(@V("text") String text);
    }

    public static void main(String[] args) {

        LocalAiChatModel model = LocalAiChatModel.builder()
                .baseUrl("http://localhost:8080")
                .modelName("gpt-4")
                .temperature(0.0)
                .build();

        PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

        String text = "In 1968, amidst the fading echoes of Independence Day, " + 
                      "a child named John arrived under the calm evening sky. " + 
                      "This newborn, bearing the surname Doe, marked the start of a new journey.";

        Person person = extractor.extractPersonFrom(text);
        System.out.println(person.firstName);
        System.out.println(person.lastName);
        System.out.println(person.birthDate);
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