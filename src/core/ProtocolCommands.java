package core;

/**
 * Created by aatanasov on 8/8/2017.
 */
public interface ProtocolCommands {

    byte COMMAND_TRY_LOGIN            = 1;

    byte COMMAND_LOGIN_RESPONSE       = 2;

    byte COMMAND_MESSAGE_PERSONAL     = 3;

    byte COMMAND_MESSAGE_GROUP        = 4;

    byte COMMAND_GET_USERS            = 5;

    byte COMMAND_USERS_REPORT         = 6;

    byte COMMAND_USER_LOGGED          = 7;

    byte COMMAND_USER_LOGGED_OUT      = 8;

    byte COMMAND_MESSAGE_RECEIVED     = 9;

    byte COMMAND_FAILURE              = 10;

}
