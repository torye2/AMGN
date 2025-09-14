package amgn.amu.dto.oauth_totp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OauthLinkStatusResponse {
    private List<String> linkedProviders;
    private boolean canUnlink;
}
