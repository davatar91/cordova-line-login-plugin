package plugin.line;

import com.linecorp.linesdk.LineAccessToken;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.LineApiResponseCode;
import com.linecorp.linesdk.LineIdToken;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.Scope;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineAuthenticationParams;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class LineLogin extends CordovaPlugin {

    private static final int REQUEST_CODE = 1;
    private static final int PARAMETER_ERROR = -1;
    private static final int SDK_ERROR = -2;
    private static final int UNKNOWN_ERROR = -3;

    private String channelId;
    private CallbackContext loginCallbackContext;
    private static LineApiClient lineApiClient;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (action.equals("initialize")) {
            this.initialize(data, callbackContext);
            return true;
        } else if (action.equals("login")) {
            this.login(callbackContext, false);
            return true;
        } else if (action.equals("loginWeb")) {
            this.login(callbackContext, true);
            return true;
        } else if (action.equals("logout")) {
            this.logout(callbackContext);
            return true;
        } else if (action.equals("getAccessToken")) {
            this.getAccessToken(callbackContext);
            return true;
        } else if (action.equals("verifyAccessToken")) {
            this.verifyAccessToken(callbackContext);
            return true;
        } else if (action.equals("refreshAccessToken")) {
            this.refreshAccessToken(callbackContext);
            return true;
        } else {
            return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            return;
        }

        CallbackContext callbackContext = loginCallbackContext;
        loginCallbackContext = null;
        if (callbackContext == null) {
            return;
        }

        LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);
        if (result.getResponseCode() == LineApiResponseCode.SUCCESS) {
            JSONObject json = new JSONObject();
            try {
                LineProfile profile = result.getLineProfile();
                if (profile != null) {
                    json.put("userID", profile.getUserId());
                    json.put("displayName", profile.getDisplayName());
                }
                if (profile != null && profile.getPictureUrl() != null) {
                    json.put("pictureURL", profile.getPictureUrl().toString());
                }

                LineIdToken lineIdToken = result.getLineIdToken();
                if (lineIdToken != null && lineIdToken.getEmail() != null) {
                    json.put("email", lineIdToken.getEmail());
                }

                callbackContext.success(json);
            } catch (JSONException e) {
                this.unknownError(callbackContext, e.toString());
            }
        } else if (result.getResponseCode() == LineApiResponseCode.CANCEL) {
            this.sdkError(callbackContext, result.getResponseCode().toString(), "user cancel");
        } else {
            this.sdkError(callbackContext, result.getResponseCode().toString(), result.toString());
        }
    }

    private void initialize(JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (data.length() == 0 || data.isNull(0)) {
            this.parameterError(callbackContext, "channel_id is required.");
            return;
        }

        JSONObject params = data.getJSONObject(0);
        channelId = params.optString("channel_id", "");
        if (channelId.length() == 0) {
            this.parameterError(callbackContext, "channel_id is required.");
        } else {
            LineApiClientBuilder apiClientBuilder = new LineApiClientBuilder(this.cordova.getActivity().getApplicationContext(), channelId);
            lineApiClient = apiClientBuilder.build();
            callbackContext.success();
        }
    }

    private void login(CallbackContext callbackContext, boolean webOnly) {
        try {
            if (!this.isInitialized()) {
                this.parameterError(callbackContext, "initialize must be called before login.");
                return;
            }

            LineAuthenticationParams authenticationParams = new LineAuthenticationParams.Builder()
                    .scopes(Arrays.asList(Scope.PROFILE, Scope.OPENID_CONNECT, Scope.OC_EMAIL))
                    .build();
            Intent loginIntent = webOnly
                    ? LineLoginApi.getLoginIntentWithoutLineAppAuth(
                            this.cordova.getActivity().getApplicationContext(),
                            channelId,
                            authenticationParams
                    )
                    : LineLoginApi.getLoginIntent(
                            this.cordova.getActivity().getApplicationContext(),
                            channelId,
                            authenticationParams
                    );
            loginCallbackContext = callbackContext;
            this.cordova.startActivityForResult((CordovaPlugin) this, loginIntent, REQUEST_CODE);
        } catch (Exception e) {
            this.unknownError(callbackContext, e.toString());
        }
    }

    private void logout(CallbackContext callbackContext) {
        this.runLineApiCall(callbackContext, new Runnable() {
            @Override
            public void run() {
                LineLogin.this.lineApiClient.logout();
                callbackContext.success();
            }
        });
    }

    private void getAccessToken(CallbackContext callbackContext) {
        this.runLineApiCall(callbackContext, new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject();
                    LineAccessToken lineAccessToken = lineApiClient.getCurrentAccessToken().getResponseData();
                    if (lineAccessToken == null) {
                        LineLogin.this.sdkError(callbackContext, "NO_ACCESS_TOKEN", "No current LINE access token.");
                        return;
                    }
                    json.put("accessToken", lineAccessToken.getTokenString());
                    json.put("expireTime", lineAccessToken.getEstimatedExpirationTimeMillis());
                    callbackContext.success(json);
                } catch (JSONException e) {
                    LineLogin.this.unknownError(callbackContext, e.toString());
                }
            }
        });
    }

    private void verifyAccessToken(CallbackContext callbackContext) {
        this.runLineApiCall(callbackContext, new Runnable() {
            @Override
            public void run() {
                LineApiResponse verifyResponse = lineApiClient.verifyToken();
                if (verifyResponse.isSuccess()) {
                    callbackContext.success();
                } else {
                    LineLogin.this.sdkError(
                            callbackContext,
                            verifyResponse.getResponseCode().toString(),
                            verifyResponse.getErrorData() == null ? "" : verifyResponse.getErrorData().toString()
                    );
                }
            }
        });
    }

    private void refreshAccessToken(CallbackContext callbackContext) {
        this.runLineApiCall(callbackContext, new Runnable() {
            @Override
            public void run() {
                LineAccessToken lineAccessToken = lineApiClient.refreshAccessToken().getResponseData();
                if (lineAccessToken == null) {
                    LineLogin.this.sdkError(callbackContext, "NO_ACCESS_TOKEN", "LINE access token could not be refreshed.");
                    return;
                }
                callbackContext.success(lineAccessToken.getTokenString());
            }
        });
    }

    private boolean isInitialized() {
        return channelId != null && channelId.length() > 0 && lineApiClient != null;
    }

    private void runLineApiCall(final CallbackContext callbackContext, final Runnable runnable) {
        if (!this.isInitialized()) {
            this.parameterError(callbackContext, "initialize must be called before calling LINE APIs.");
            return;
        }

        this.cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LineLogin.this.unknownError(callbackContext, e.toString());
                }
            }
        });
    }

    private void parameterError(CallbackContext callbackContext, String description) {
        try {
            JSONObject json = new JSONObject();
            json.put("code", PARAMETER_ERROR);
            json.put("description", description);
            callbackContext.error(json);
        } catch (JSONException e) {
            this.unknownError(callbackContext, e.toString());
        }
    }

    private void sdkError(CallbackContext callbackContext, String sdkErrorCode, String description) {
        try {
            JSONObject json = new JSONObject();
            json.put("code", SDK_ERROR);
            json.put("sdkErrorCode", sdkErrorCode);
            json.put("description", description);
            callbackContext.error(json);
        } catch (JSONException e) {
            this.unknownError(callbackContext, e.toString());
        }
    }

    private void unknownError(CallbackContext callbackContext, String description) {
        try {
            JSONObject json = new JSONObject();
            json.put("code", UNKNOWN_ERROR);
            json.put("description", description);
            callbackContext.error(json);
        } catch (JSONException e) {
            callbackContext.error(-1);
        }
    }
}
