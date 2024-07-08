## Data Extraction Experiments

# Let's serve the model with Ollama:

In a terminal, execute commands as below:

```
MODEL_NAME=codellama
docker run -p 11434:11434 langchain4j/ollama-${MODEL_NAME}:latest
```

# Let's start with Open Extraction

With `MainOpenExtractCodeLlama`, we ask the model to output JSON only.
We do not mandate anything more about the expected structure.
As such, the model is able to propose some JSON structure on its own, let's review below what is generated:

```
{
  "name": "Sarah London",
  "birth date": "1986-07-10"
}
----- Inference lasted 11.3s ------------------------------
{
  "Customer": "John Doe",
  "Date of Birth": "November 1st, 2001",
  "Insurance Company": "Operator\u0027s company",
  "Reimbursement Amount": "Half the money spent due to the accident",
  "Full Reimbursement Option": "Cancelled automatically by our system",
  "Automatic Renewal": "Not interested in automatic renewal at the time of subscription",
  "Notification": "Not notified about the automatic change",
  "Manager": "Operator\u0027s manager"
}
----- Inference lasted 24.0s ------------------------------
{
  "Name": "Kate Hart",
  "Birth Date": "August 13, 1999",
  "Last Name": "Hart",
  "New Last Name": "Boss",
  "Married": true
}
----- Inference lasted 17.2s ------------------------------
```

Of course the proposed JSON schema is not the same and depends upon the input conversation.
It's interesting to see how the semantic meaning of the text influences the proposed structure.

# Let's switch to closed extraction

In `MainExtractJsonCodeLlama`, we now force the output JSON schema as below:

```
{
 conversation:{
  emotions: {
   customerSatisfied: (type: boolean)
  },
  entities: {
   id: {
    customerName: (type: string)
   },
   attributes: {
    customerBirthday: (type: date string)
   }
  },
  summary: (type: string)
 }
}
```

See how the expected structure has multiple levels.

We intend to gather:
 + simple values like `customerName` and `customerSatisfied`
 + a formatted value like `customerBirthday` with expected date format `DD-MM-YYYY` specified in the prompt
 + an unstructured text like `summary`

See below how well it behaves:

```
{
  "conversation": {
    "emotions": {
      "customerSatisfied": true
    },
    "entities": {
      "id": {
        "customerName": "Sarah London"
      },
      "attributes": {
        "customerBirthday": "10-07-1986"
      }
    },
    "summary": "Declaring an accident on main vehicle."
  }
}
----- Inference lasted 20.1s ------------------------------
{
  "conversation": {
    "emotions": {
      "customerSatisfied": false
    },
    "entities": {
      "id": {
        "customerName": "John Doe"
      },
      "attributes": {
        "customerBirthday": "01-11-2001"
      }
    },
    "summary": "Customer is unhappy with the automatic cancellation of their full reimbursement option."
  }
}
----- Inference lasted 25.3s ------------------------------
{
  "conversation": {
    "emotions": {
      "customerSatisfied": true
    },
    "entities": {
      "id": {
        "customerName": "Kate Boss"
      },
      "attributes": {
        "customerBirthday": "13-08-1999"
      }
    },
    "summary": "Customer Kate Boss is at the police station with an accident and needs proof of insurance."
  }
}
----- Inference lasted 27.8s ------------------------------
```

# Let's generate Result<String> in order to retrieve sources and tokenUsage

With `MainExtractResultCodeLlama`, we then specify a lighter output JSON schema:

```
{
 customerSatisfied: (type: boolean),
 customerName: (type: string),
 customerBirthday: (type: date string)
}
```

And ask the result to be provided as `Result<String>`.
Let's see the produced output below:

```
****************************************
content: {
"customerSatisfied": true,
"customerName": "Sarah London",
"customerBirthday": "10-07-1986"
}
sources: null
tokenUsage: TokenUsage { inputTokenCount = 256, outputTokenCount = 43, totalTokenCount = 299 }
****************************************

----- Inference lasted 12.8s ------------------------------
****************************************
content: {
"customerSatisfied": false,
"customerName": "John Doe",
"customerBirthday": "01-11-2001"
}
sources: null
tokenUsage: TokenUsage { inputTokenCount = 383, outputTokenCount = 42, totalTokenCount = 425 }
****************************************

----- Inference lasted 16.4s ------------------------------
****************************************
content: {
"customerSatisfied": true,
"customerName": "Kate Boss",
"customerBirthday": "13-08-1999"
}
sources: null
tokenUsage: TokenUsage { inputTokenCount = 383, outputTokenCount = 43, totalTokenCount = 426 }
****************************************

----- Inference lasted 18.0s ------------------------------
```

As can be seen, we now have more information about the chat completion.
Note that sources is <code>null</code> here as we don't use Retrieval Augmentated Generation.

# Let's extract Java objects like Enum and List

`Langchain4j` also allows to return custom pojos. This setup is tested in `MainExtractEnumAndListCodeLlama` as shown below:

```
    enum GENDER {
        MALE,
        FEMALE
    }

    static class EnumAndListPojo {
        private GENDER gender;
        private List<String> topics;
    }
```

The JSON schema is now automatically created by langchain4j and silently injected in the prompt. The results are shown below:

```
****************************************
gender: FEMALE
topics: [accident, vehicle]
****************************************

----- Inference lasted 8.7s ------------------------------
****************************************
gender: MALE
topics: [insurance, accident, reimbursement, automatic renewal]
****************************************

----- Inference lasted 14.4s ------------------------------
****************************************
gender: FEMALE
topics: [insurance, accident, police station]
****************************************

----- Inference lasted 14.8s ------------------------------
```

# Let's define a more complex Pojo

In `MainExtractCustomPojoCodeLlama`, we merge some complexities from previous experiments.

```
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
```

See how all fields are extracted thanks to the setup:

```
****************************************
customerSatisfied: true
customerName: Sarah London
customerBirthday: 10 July 1986
summary: Declare an accident on main vehicle and receive reimbursement for expenses.
****************************************

----- Inference lasted 13.5s ------------------------------
****************************************
customerSatisfied: false
customerName: John Doe
customerBirthday: 01 November 2001
summary: Customer is not satisfied with the automatic cancellation of the full reimbursement option and wants to be notified about changes in their contract.
****************************************

----- Inference lasted 21.1s ------------------------------
****************************************
customerSatisfied: true
customerName: Kate Boss
customerBirthday: 13 August 1999
summary: Customer Kate Boss is satisfied with the assistance provided by the operator. The customer was able to provide their name and birth date correctly, and the operator was able to locate their insurance contract.
****************************************

----- Inference lasted 23.8s ------------------------------
```

# Testing with other models

Note that it's possible to experiment with data extraction against other models as below:

```
MODEL_NAME=llama3
docker run -p 11434:11434 langchain4j/ollama-${MODEL_NAME}:latest
```

`MainExtractCustomPojoLlama3` will then output:

```
****************************************
customerSatisfied: true
customerName: Sarah London
customerBirthday: 10 July 1986
summary: Declared an accident on main vehicle and sought reimbursement for expenses
****************************************

----- Inference lasted 15.2s ------------------------------
****************************************
customerSatisfied: false
customerName: John Doe
customerBirthday: 01 November 2001
summary: Reimbursement of only half the money spent due to an accident
****************************************

----- Inference lasted 27.4s ------------------------------
****************************************
customerSatisfied: true
customerName: Kate Boss
customerBirthday: 13 August 1999
summary: Request for proof of insurance due to an accident and need for police report
****************************************

----- Inference lasted 27.9s ------------------------------
```

And finally, with mistral.

```
MODEL_NAME=mistral
docker run -p 11434:11434 langchain4j/ollama-${MODEL_NAME}:latest
```

The outcome of `MainExtractCustomPojoMistral` is found below:

```
****************************************
customerSatisfied: true
customerName: Sarah London
customerBirthday: 10 July 1986
summary: Customer Sarah London called to declare an accident on her main vehicle and was informed that all expenses will be reimbursed.
****************************************

----- Inference lasted 19.3s ------------------------------
****************************************
customerSatisfied: false
customerName: John Doe
customerBirthday: 01 November 2001
summary: Customer John Doe is dissatisfied as the insurance company reimbursed only half of his expenses due to an accident and he was not notified about the cancellation of the full reimbursement option.
****************************************

----- Inference lasted 39.7s ------------------------------
****************************************
customerSatisfied: true
customerName: Kate Boss
customerBirthday: 13 August 1999
summary: Customer Kate Boss needed proof of insurance due to an accident and was assisted by the operator.
****************************************

----- Inference lasted 32.7s ------------------------------
```
