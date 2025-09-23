package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteAccountRequest {
    private String password;
    private String reason;
    private boolean wipeConvenienceData = true;
}
