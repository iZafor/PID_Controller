package code.z.pidcontroller

enum class Commands(val value: Char) {
    KP_INCREMENT('P'),
    KP_DECREMENT('Q'),
    KD_INCREMENT('D'),
    KD_DECREMENT('E'),
    KI_INCREMENT('I'),
    KI_DECREMENT('J'),
    FACTOR_UPSCALE('U'),
    FACTOR_DOWNSCALE('V'),
    START('S'),
    STOP('T');
}
