package in.petsfort.crm;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class InMemoryFirebaseService implements FirebaseService {
    final Map<String,Object> root=new LinkedHashMap<>();
    final Map<String,String> users=new LinkedHashMap<>();
    final List<Map<String,Object>> notifications=new ArrayList<>();
    final List<String> uploads=new ArrayList<>();
    public boolean enabled(){return true;}
    public String createUser(String email,String password){String uid="uid-"+UUID.randomUUID().toString().substring(0,8);users.put(uid,email+":"+password);return uid;}
    public void updatePassword(String uid,String password){users.put(uid,users.get(uid).split(":",2)[0]+":"+password);}
    public void deleteUser(String uid){users.remove(uid);}
    public void notifyTopic(String topic,String title,String body,Map<String,String>data){Map<String,Object>n=new LinkedHashMap<>();n.put("topic",topic);n.put("title",title);n.put("body",body);n.put("data",data);notifications.add(n);}
    public synchronized void set(String path,Object value){String[]parts=path.split("/");Map<String,Object>node=root;for(int i=0;i<parts.length-1;i++)node=(Map<String,Object>)node.computeIfAbsent(parts[i],k->new LinkedHashMap<>());node.put(parts[parts.length-1],deepCopy(value));}
    public synchronized Object get(String path){Object node=root;for(String part:path.split("/")){if(!(node instanceof Map))return null;node=((Map<?,?>)node).get(part);if(node==null)return null;}return deepCopy(node);}
    public synchronized void delete(String path){String[]parts=path.split("/");Map<String,Object>node=root;for(int i=0;i<parts.length-1;i++){Object next=node.get(parts[i]);if(!(next instanceof Map))return;node=(Map<String,Object>)next;}node.remove(parts[parts.length-1]);}
    public String verifyEmail(String token){return token.equals("admin-token")?BackupService.ADMIN_EMAIL:"someone@example.test";}
    public JsonObject upload(Path file,String name){uploads.add(name);JsonObject o=new JsonObject();o.addProperty("path",name);o.addProperty("url","memory://"+name);o.add("err",null);return o;}
    public void close(){}
    private static Object deepCopy(Object value){return new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(value),Object.class);}
}
