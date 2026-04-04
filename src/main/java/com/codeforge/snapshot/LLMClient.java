package com.codeforge.snapshot;

public interface LLMClient {

    boolean isConfigured();

    String complete(String systemPrompt, String userPrompt) throws Exception;
}
