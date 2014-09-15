package bzh.ygu.fun.chitchat.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.query.UpdateResults;


public class Root {
	
	public String id;
	private TreeSet<Message> messages = new TreeSet<Message>();
	private TreeSet<Hero> heros = new TreeSet<Hero>();
	private Map<Integer, MessageThread> mapThread = new HashMap<Integer, MessageThread>(); 
	
	private static Root instance;
//	private static MongoDao dao;
	private static Datastore ds;
	public static void createInstance() {
		
		instance = new Root();
		
	}
	public static Root getInstance() {
		return instance;
	}
	public void addMessage(String author, String text, String thread) {
		
		Hero hero = storeHero(author);
		Key<Hero> ksender = ds.getKey(hero);
		int threadNumber = getThreadNumber(thread);
		Message msg = new Message(hero, text,threadNumber,System.currentTimeMillis());
		Key<Message> kmsg = ds.save(msg);
		//add Hero  as the sender of his msg
		UpdateResults<Message> res =
		  ds.update(
		    msg,
		    ds.createUpdateOperations(Message.class).set("author", ksender)
		  );
		messages.add(msg);
		addMessageToThread(threadNumber, kmsg,msg);
		
		
	}
	private void addMessageToThread(int threadNumber, Key<Message> kmsg,Message msg) {
		if (threadNumber >=0) {
			Integer thread = new Integer(threadNumber);
			if (mapThread.containsKey(thread)) {
				mapThread.get(thread).add(msg);
			} else {
				MessageThread theThread = null;
			
				if (mapThread.containsKey(thread)) {
					theThread = mapThread.get(thread);
				} else {
					theThread= new MessageThread(thread);
					ds.save(theThread);
					mapThread.put(thread, theThread);
				}
				if ( theThread != null) {
					ds.update(theThread,ds.createUpdateOperations(MessageThread.class).add("messages", kmsg));
					theThread.add(msg);
				}
			}
		}
		
	}
	private int getThreadNumber(String thread) {
		if (thread == null) {
			return -1;
		}
		try {
			return Integer.parseInt(thread);
		}catch (NumberFormatException nfe) {
			return -1;
		}
		
	}
	private Hero storeHero(String name) {
		Hero newHero = new Hero(name);
		Hero hero = null;
		if(heros.contains(newHero)) {
			hero = ds.find(Hero.class).field("name").equal(name).get();
			
		} else {
		ds.save(newHero);
		hero = ds.find(Hero.class).field("name").equal(name).get();
		heros.add(hero);
		}
		return hero;
	}

	public Message getLatest(String latestFromWho) {
		Key<Hero> khero = ds.find(Hero.class).field("name").equal(latestFromWho).getKey();
		return ds.find(Message.class).field("author").equal(khero).order("-createdAt").limit(1).get();
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
//	public static void setDao(MongoDao newDao) {
//		dao = newDao;
//		
//	}
	public TreeSet<Message> getMessages() {
		return messages;
	}
	public TreeSet<Hero> getHeros() {
		return heros;
	}
	public Map<Integer, MessageThread> getMapThread() {
		return mapThread;
	}
	public void setMessages(TreeSet<Message> messages) {
		this.messages = messages;
	}
	public void setHeros(TreeSet<Hero> heros) {
		this.heros = heros;
	}
	public void setMapThread(Map<Integer, MessageThread> mapThread) {
		this.mapThread = mapThread;
	}
	public static void setDS(Datastore ds) {
		Root.ds = ds;
		
	}
	public MessageThread getMessageThread(String threadId) {
		int id = Integer.parseInt(threadId);
		//ds.find(MessageThread.class).field("threadId").equal(id).get();
		return mapThread.get(id);
		
	}
	public List<Message> search(String stringToSearch) {
		return ds.find(Message.class).field("text").containsIgnoreCase(stringToSearch).asList();
	}
	
}
