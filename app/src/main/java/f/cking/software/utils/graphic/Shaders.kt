package f.cking.software.utils.graphic

import org.intellij.lang.annotations.Language

object Shaders {
    @Language("AGSL")
    val SHADER_CONTENT = """
        uniform shader content;
    
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        float4 main(float2 coord) {
            if (coord.y > iResolution.y - blurredHeight) { // Blur the bottom part of the screen
                return float4(1.0, 1.0, 1.0, 1.0);
            } else {
                return content.eval(coord);
            }
        }
"""

    @Language("AGSL")
    val SHADER_BLURRED = """
        uniform shader content;
    
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        float4 main(float2 coord) {
            if (coord.y > iResolution.y - blurredHeight) { // Blur the bottom part of the screen
                return content.eval(coord);
            } else {
                return float4(0.0, 0.0, 0.0, 0.0);
            }
        }
    """

    @Language("AGSL")
    val GLASS_SHADER = """
        uniform shader content;
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        uniform float horizontalSquareSize;
        const float verticalSquares = 1.0;
        const float verticalOffset = 0.1;
        const float horizontalOffset = 0.05;
        
        const float amt = 0.1;
        
        float4 gradient(float2 coordOriginal) {
            float2 coord = float2(coordOriginal.x, coordOriginal.y - iResolution.y * 0.5 + blurredHeight);
            float2 pos_ndc = 2.0 * coord.xy / iResolution.xy - 1.0;
            float dist = length(pos_ndc);
        
            vec4 color1 = vec4(0.0, 0.0, 0.0, 1.0);
            vec4 color2 = vec4(0.95, 0.95, 0.95, 1.0);
            vec4 color3 = vec4(0.0, 0.0, 0.0, 1.0);
            vec4 color4 = vec4(0.95, 0.95, 1.0, 0.95);
            float step1 = 0.0;
            float step2 = 0.33;
            float step3 = 0.66;
            float step4 = 1.0;
        
            vec4 color = mix(color1, color2, smoothstep(step1, step2, dist));
            color = mix(color, color3, smoothstep(step2, step3, dist));
            color = mix(color, color4, smoothstep(step3, step4, dist));
        
            return color;
        }

        float4 main(float2 fragCoord) {
            float2 offset = float2(horizontalOffset, verticalOffset);
            float2 squares = float2(iResolution.x / horizontalSquareSize, verticalSquares);
        	float2 uv = fragCoord.xy / iResolution.xy;
            
            float2 tc = uv;
            tc.x *= iResolution.x / iResolution.y;
            
            float2 tile = fract(tc * squares);
            
            float4 color = content.eval((uv + (tile * amt) - offset) * iResolution.xy);
            float4 white = float4(1.0, 1.0, 1.0, 1.0);
            float4 colorModificator = 0.04 * gradient((uv + (tile * amt) - offset) * iResolution.xy);
        	return min(color + colorModificator, white);
        }
    """
}