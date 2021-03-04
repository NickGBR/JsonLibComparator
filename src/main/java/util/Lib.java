package util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Lib {
    String name;
    String version;
}
