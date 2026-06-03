import Foundation
import LineSDK

@objc(LineLogin) class Line : CDVPlugin {
    
    struct ErrorCode {
        static let ParameterError = -1
        static let SDKError = -2
        static let UnknownError = -3
    }

    @objc func initialize(_ command: CDVInvokedUrlCommand) {
        
        guard command.arguments.count > 0, let params = command.arguments[0] as? [String: Any] else {
            self.parameterError(command: command, description: "channel_id is required")
            return
        }
        
        guard let channelID = params["channel_id"] as? String, !channelID.isEmpty else {
            self.parameterError(command: command, description: "channel_id is required")
            return
        }
        
        if !LoginManager.shared.isSetupFinished {
            LoginManager.shared.setup(channelID: channelID, universalLinkURL: nil)
        }
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.send(result, callbackId:command.callbackId)
    }
    
    func _login(_ command: CDVInvokedUrlCommand, onlyWebLogin: Bool) {
        guard LoginManager.shared.isSetupFinished else {
            self.parameterError(command: command, description: "initialize must be called before login")
            return
        }

        var parameters = LoginManager.Parameters()
        parameters.onlyWebLogin = onlyWebLogin
        LoginManager.shared.login(permissions: [.profile, .openID, .email], in: self.viewController, parameters: parameters) {
            result in
            switch result {
            case .success(let loginResult):
                var data = [String : Any]()
                
                if let displayName = loginResult.userProfile?.displayName {
                    data["displayName"] = displayName
                }
                if let userID = loginResult.userProfile?.userID {
                    data["userID"] = userID
                }
                if let email = loginResult.accessToken.IDToken?.payload.email {
                    data["email"] = email
                }
                if let pictureURL = loginResult.userProfile?.pictureURL {
                    data["pictureURL"] = String(describing: pictureURL)
                }
                
                let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs:data)
                self.commandDelegate.send(result, callbackId:command.callbackId)
            case .failure(let error):
                self.sdkError(command: command, error: error)
            }
        }
    }
    
    @objc func loginWeb(_ command: CDVInvokedUrlCommand) {
        self._login(command, onlyWebLogin: true)
    }
    
    @objc func login(_ command: CDVInvokedUrlCommand) {
        self._login(command, onlyWebLogin: false)
    }
    
    @objc func logout(_ command: CDVInvokedUrlCommand) {
        LoginManager.shared.logout { result in
            switch result {
            case .success:
                let result = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate.send(result, callbackId:command.callbackId)
            case .failure(let error):
                self.sdkError(command: command, error: error)
            }
        }
    }

    @objc func getAccessToken(_ command: CDVInvokedUrlCommand) {
        
        guard let currentAccessToken = AccessTokenStore.shared.current else {
            self.sdkError(command: command, sdkErrorCode: "NO_ACCESS_TOKEN", description: "No current LINE access token.")
            return
        }
        
        let data = ["accessToken":currentAccessToken.value, "expireTime":currentAccessToken.expiresAt.timeIntervalSince1970] as [String : Any]
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs:data)
        commandDelegate.send(result, callbackId:command.callbackId)
    }
    
    @objc func verifyAccessToken(_ command: CDVInvokedUrlCommand) {
        
        API.Auth.verifyAccessToken { (result) in
            switch result {
            case .success( _):
                let result = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate.send(result, callbackId:command.callbackId)
            case .failure(let error):
                self.sdkError(command: command, error: error)
            }
        }
    }
    
    @objc func refreshAccessToken(_ command: CDVInvokedUrlCommand) {

        API.Auth.refreshAccessToken { (result) in
            switch result {
            case .success(let accessToken):
                let data = ["accessToken":accessToken.value, "expireTime":accessToken.expiresAt.timeIntervalSince1970] as [String : Any]
                let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs:data)
                self.commandDelegate.send(result, callbackId:command.callbackId)
            case .failure(let error):
                self.sdkError(command: command, error: error)
            }
        }
    }
    
    private func parameterError(command: CDVInvokedUrlCommand, description: String) {
        let err = ["code":ErrorCode.ParameterError, "description": description] as [AnyHashable : Any]
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: err)
        self.commandDelegate.send(result, callbackId:command.callbackId)
    }
    
    private func sdkError(command: CDVInvokedUrlCommand, error: LineSDKError) {
        let description = error.errorDescription ?? ""
        self.sdkError(command: command, sdkErrorCode: String(error.errorCode), description: description)
    }

    private func sdkError(command: CDVInvokedUrlCommand, sdkErrorCode: String, description: String) {
        let err = ["code":ErrorCode.SDKError, "sdkErrorCode":sdkErrorCode, "description": description] as [AnyHashable : Any]
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: err)
        self.commandDelegate.send(result, callbackId:command.callbackId)
    }
    
}
