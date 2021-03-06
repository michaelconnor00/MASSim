package tests.comm;


import massim.CommMedium;
/**
 * Test class for the communication medium (CommMedium.java)
 * 
 * @author Omid Alemi
 *
 */
public class CommTester {
	
	public static void main(String[] args) {
		
		CommMedium commMedium = new CommMedium(4);
		String msg1 = "Hello";
		String msg2 = "2,3,map,bid,234";
		
		if (commMedium.isEmpty())
			System.out.println("THE COMM MED IS EMPTY!");
		
		System.out.println("The buffers for Agent 3:");
		printBuffer(commMedium.receive(3));
		
		System.out.println("Agent 2 sends a message to Agent 3");
		commMedium.send(2, 3, msg2);
		
		System.out.println("The buffers for Agent 3:");
		printBuffer(commMedium.receive(3));
		
		//
		System.out.println("Agent 1 broadcast a message");
		commMedium.broadcast(1, msg1);
				
		
		System.out.println("The buffers for Agent 0:");
		printBuffer(commMedium.receive(0));
		
		System.out.println("The buffers for Agent 1:");
		printBuffer(commMedium.receive(1));
		
		System.out.println("The buffers for Agent 2:");
		printBuffer(commMedium.receive(2));
		
		System.out.println("The buffers for Agent 3:");
		printBuffer(commMedium.receive(3));
		
		if (commMedium.isEmpty())
			System.out.println("THE COMM MED IS EMPTY!");
	}

	public static void printBuffer(String[] buffer) {		
		for (int i=0;i<buffer.length;i++)
		{
			System.out.print("Agent "+ i+ " : ");
			System.out.println(buffer[i]);
		}
	}
}
