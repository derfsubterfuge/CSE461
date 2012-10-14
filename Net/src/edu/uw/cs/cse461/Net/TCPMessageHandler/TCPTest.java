package edu.uw.cs.cse461.Net.TCPMessageHandler;

public class TCPTest {

	public static void main(String[] args) {
		
		// test byteToInt and intToByte
		int i = 42;
		System.out.println("Integer before conversion = " + i);
		byte[] b = TCPMessageHandler.intToByte(i);
		int j = TCPMessageHandler.byteToInt(b);
		System.out.println("Integer after conversion = " + j);

		
		
		
	}

}
