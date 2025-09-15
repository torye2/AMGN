package amgn.amu.dto.oauth_totp;

import amgn.amu.dto.LoginUserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OauthLoginResult {
    public enum Status {
        LOGGED_IN,
        LINKED_AND_LOGGED_IN,
        SIGNUP_CREATED
    }

    private Status status;
    private LoginUserDto loginUser;
}
