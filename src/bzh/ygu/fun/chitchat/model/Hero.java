package bzh.ygu.fun.chitchat.model;

import java.util.Set;
import java.util.TreeSet;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;



@Entity("heros")
public class Hero implements Comparable<Hero>{

	@Id
    private ObjectId id;
	
	private String name;
	@Reference
	private Set<Message> messages = new TreeSet<Message>();;
	
	public Hero() {
		super();
	}
	
	public Hero(String name) {
		this.name = name;
	}
	
	
	public  String getName() {
		return name;
	}
	public  void setName(String newName) {
		 name = newName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Hero other = (Hero) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}


	public Set<Message> getMessages() {
		return messages;
	}


	public void setMessages(Set<Message> messages) {
		this.messages = messages;
	}


	public boolean add(Message e) {
		return messages.add(e);
	}

	@Override
	public int compareTo(Hero o) {
		
		return this.name.compareTo(o.name);
	}
	
	
}
