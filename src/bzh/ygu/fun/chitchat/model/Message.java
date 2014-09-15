package bzh.ygu.fun.chitchat.model;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.IndexDirection;


@Entity("messages")
public  class Message  implements Comparable<Message>{
	
	@Id
	public ObjectId id;
	@Indexed
	private  String text;
	@Indexed(value=IndexDirection.DESC)
	private  long createdAt;
	@Reference
	@Indexed
	private  Hero author;
	private  int threadId;
	
	public Message() {
		super();
	}

	public Message(Hero author, String text,int threadId, long createdAt) {
		super();
		this.author = author;
		this.text = text;
		this.createdAt = createdAt;
		this.threadId = threadId;
	}

	public  String getText() {
		return text;
	}

	public  long getCreatedAt() {
		return createdAt;
	}
	
	public  Hero getAuthor() {
		return author;
	}
	
	public  int getThreadId() {
		return threadId;
	}

	public final void setText(String text) {
		this.text = text;
	}

	public final void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public final void setAuthor(Hero author) {
		this.author = author;
	}

	public final void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public  ObjectId getId() {
		return id;
	}

	public  void setId(ObjectId id) {
		this.id = id;
	}

	@Override
	public int compareTo(Message o) {	
		return (int)(this.createdAt - o.getCreatedAt());
	}	
	
	
	public String toJSON() {
		StringBuffer msgBuf = new StringBuffer();
			//{"author":"Iron Man", "text":"We have a Hulk !", "thread":3,"createdAt":1404736639715}
			msgBuf.append("{\"author\":\"");
			msgBuf.append(this.getAuthor().getName());
			msgBuf.append("\", \"text\":\"");
			msgBuf.append(this.getText());
			msgBuf.append("\", \"thread\":");
			msgBuf.append(this.getThreadId());
			msgBuf.append(", \"createdAt\":");
			msgBuf.append(this.getCreatedAt());
			msgBuf.append("}");
			return msgBuf.toString();
		
	}
}
