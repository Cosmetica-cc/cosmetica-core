/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.core.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Indicates a login success or error, and forwards error messages for unsuccessful logins to the caller.
 */
public class LoginResult {
    public LoginResult(boolean success, @NotNull Code code, @NotNull String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    private final boolean success;
    private final Code code;
    private final String message;

    public boolean isSuccess() {
        return this.success;
    }

    public Code getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "LoginResult{" +
                "success=" + success +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public enum Code {
        // Local
        SUCCESS,
        MOJANG_LOGIN_ERROR,
        GENERIC_KEY_ERROR,
        GENERIC_VERIFY_ERROR,
        ENCRYPTION_ERROR,

        // Api Responses
        GET_AUTH_SERVER_ERROR,
        INVALID_REQUEST_BODY_KEY,
        INVALID_USERNAME_KEY,
        INVALID_REQUEST_BODY_VERIFY,
        INVALID_USERNAME_VERIFY,
        INVALID_SESSION_ID,
        INVALID_VERIFY_TOKEN,
        CRACKED_MINECRAFT_UNSUPPORTED,
        INVALID_MOJANG_RESPONSE,
        INVALID_UPSTREAM_RESPONSE;

        public static Code forKeyApi(String apiCode) {
            switch (apiCode) {
            case "INVALID_REQUEST_BODY":
                return INVALID_REQUEST_BODY_KEY;
            case "INVALID_USERNAME":
                return INVALID_USERNAME_KEY;
            default:
                return Code.valueOf(apiCode);
            }
        }

        public static Code forVerifyApi(String apiCode) {
            switch (apiCode) {
            case "INVALID_REQUEST_BODY":
                return INVALID_REQUEST_BODY_VERIFY;
            case "INVALID_USERNAME":
                return INVALID_USERNAME_VERIFY;
            default:
                return Code.valueOf(apiCode);
            }
        }
    }
}
