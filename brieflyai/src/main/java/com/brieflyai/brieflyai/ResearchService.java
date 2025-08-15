package com.brieflyai.brieflyai;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;


//as this class is a service class, it has all the business logic
//meaning all the functions
//this class would be called by controller
@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    //creating web client instance
    private final WebClient webclient;
    private final ObjectMapper objectMapper;
    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper){
        this.webclient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

   
    public String processContent(ResearchRequest request){
        //build the prompt
        String prompt = buildPrompt(request);
        //query the ai model api
        Map<String,Object> requestBody = Map.of(
            "contents",new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text",prompt)
                })
            }
        );

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured. Set the environment variable or .env value.");
        }

        String response = webclient
            .post()
            .uri(geminiApiUrl)
            .header("x-goog-api-key", geminiApiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        //parse the response
        //return response
        return extractTextFromResponse(response);
        
    }
    private String extractTextFromResponse(String response){
        try{
            //converting response(json) to java object
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()){
                GeminiResponse.Candidate firsCandidate = geminiResponse.getCandidates().get(0);
                if(firsCandidate.getContent()!=null && firsCandidate.getContent().getParts()!=null && !firsCandidate.getContent().getParts().isEmpty()){
                    return firsCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No text found";

        }catch(Exception e){
            return "Error" + e.getMessage();
        }
    }
    private String buildPrompt(ResearchRequest request){
        StringBuilder prompt = new StringBuilder();
        switch(request.getOperation()){
            case "summarize":
                prompt.append("Provide a concise and clear summary of the following text in a few bullets \n \n");
                break;
            case "suggest":
                prompt.append("based on the following topic suggest related topics and further reading. Format the response with clear headings and bullet points: the answer format should not be markdown \n \n");
                break;
            case "ask":
                prompt.append("based on the following topic answer the question asked in simple words. Format the response with clear headings and bullet points: the answer format should not be markdown \n \n");
                break;
            default:
                throw new IllegalArgumentException("Unknown Operation" + request.getOperation());
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }
}
