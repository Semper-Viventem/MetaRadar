package f.cking.software.utils.graphic

import org.intellij.lang.annotations.Language

object Shaders {
    @Language("AGSL")
    val SHADER_CONTENT = """
        uniform shader content;
        uniform shader blur;
    
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
}