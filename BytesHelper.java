public final class BytesHelper {

    private BytesHelper(){}

    public static int toInt( byte[] b ) {
	return (b[3] << 24)
	    + ((b[2] & 0xFF) << 16)
	    + ((b[1] & 0xFF) << 8)
	    + (b[0] & 0xFF);
    }

    public static long toUint( byte[] b ) {
	return ((b[3] << 24) & 0xFF)
	    + ((b[2] & 0xFF) << 16)
	    + ((b[1] & 0xFF) << 8)
	    + (b[0] & 0xFF);
    }

    public static long toUintRev( byte[] b ) {
	return ((b[0] << 24) & 0xFF)
	    + ((b[1] & 0xFF) << 16)
	    + ((b[2] & 0xFF) << 8)
	    + (b[3] & 0xFF);
    }

    public static int shortToInt( byte[] b ) {
	return ((b[1]) << 8)
	    + (b[0] & 0xFF);
    }
    
    public static long shortToUint( byte[] b ) {
	return ((b[1] & 0xFF) << 8)
	    + (b[0] & 0xFF);
    }
    
}