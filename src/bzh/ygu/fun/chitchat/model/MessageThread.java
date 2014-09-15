package bzh.ygu.fun.chitchat.model;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Reference;

@Entity("messageThreads")
public class MessageThread {

	@Id
	public ObjectId id;
	@Indexed
	private  int threadId;
	@Reference
	TreeSet<Message> messages = new TreeSet<Message>();
	
	public MessageThread() {
		super();
	}
	
	public MessageThread(Integer thread) {
		threadId = thread;
	}
	public int getThreadId() {
		return threadId;
	}
	public Set<Message> getMessages() {
		return messages;
	}
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
	
	public void setMessages(TreeSet<Message> messages) {
		this.messages = messages;
	}
	public boolean add(Message e) {
		return messages.add(e);
	}

	public ObjectId getId() {
		return id;
	}
	public void setId(ObjectId id) {
		this.id = id;
	}

	public Object toJSON() {
		StringBuffer msgBuf = new StringBuffer();
		//[{"author":"Loki", "text":"I have an army !", "thread":3,
		//"createdAt":1404736639710}, {"author":"Iron Man", "text":"We have a Hulk !",
		//	"thread":3, "createdAt":1404736639715}]
		msgBuf.append("[");
		if ( ! messages.isEmpty()) {
			Iterator<Message> it = messages.iterator();
			Message m = it.next();
			msgBuf.append(m.toJSON());
			while(it.hasNext()) {
				m = it.next();
				msgBuf.append(", ");
				msgBuf.append(m.toJSON());
			}
		}
		
		msgBuf.append("]");
		return msgBuf.toString();
	}
	
}
