/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package bzh.ygu.fun.chitchat;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Iterator;
import java.util.List;

import sun.nio.cs.ThreadLocalCoders;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import bzh.ygu.fun.chitchat.model.Message;
import bzh.ygu.fun.chitchat.model.MessageThread;
import bzh.ygu.fun.chitchat.model.Root;

public class HttpChitChatServerHandler extends SimpleChannelInboundHandler<Object> {

    private HttpRequest request;
    /** Buffer that stores the response content */
    private final StringBuilder buf = new StringBuilder();
    private final StringBuilder contentBuf = new StringBuilder();
    
    

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
    
    private final String LATEST = "/chitchat/latest/";
    private final int LATEST_SIZE = 17;
    private final String THREADCALL = "/chitchat/thread/";
    private final int THREADCALL_SIZE = 17;
    private final String SEARCH= "/chitchat/search?q=";
    private final int SEARCH_SIZE = 19;
    private String currentAction = "None";
    private String latestFromWho = null;
   
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }
            String uri = request.uri();
            HttpMethod method = request.method();
            Root root = Root.getInstance();
            buf.setLength(0);
            contentBuf.setLength(0);

            if (method == HttpMethod.POST) {
            	if (uri.equals("/chitchat")) {
            		
            		currentAction = "Add";
            	}
            }
            if (method == HttpMethod.GET) {
            	if (uri.startsWith(LATEST)) {
            		latestFromWho = decode(uri.substring(LATEST_SIZE));
            		currentAction = "Latest";
            		Message m = root.getLatest(latestFromWho);
            		if (m != null) {
            			//{"author":"Iron Man", "text":"We have a Hulk !", "thread":3,"createdAt":1404736639715}
            			buf.append(m.toJSON());
            		}
            	}
            	if (uri.startsWith(THREADCALL)) {
            		currentAction = "Thread";
            		String threadId = uri.substring(THREADCALL_SIZE);
            		MessageThread mt = root.getMessageThread(threadId);
            		if (mt != null) {
            			//{"author":"Iron Man", "text":"We have a Hulk !", "thread":3,"createdAt":1404736639715}
            			buf.append(mt.toJSON());
            		}            		
            		
            	}
            	if (uri.startsWith(SEARCH)) {
            		String stringToSearch = decode(uri.substring(SEARCH_SIZE));
            		currentAction = "search";
            		List<Message> lm = root.search(stringToSearch);
            		//[{"author":"Loki", "text":"I have an army !", "thread":3,
            		//"createdAt":1404736639710}, {"author":"Iron Man", "text":"We have a Hulk !",
            		//	"thread":3, "createdAt":1404736639715}]
            		buf.append("[");
            		if ( ! lm.isEmpty()) {
            			Iterator<Message> it = lm.iterator();
            			Message m = it.next();
            			buf.append(m.toJSON());
            			while(it.hasNext()) {
            				m = it.next();
            				buf.append(", ");
            				buf.append(m.toJSON());
            			}
            		}
            		
            		buf.append("]");
            	}
            }
        }

        if (msg instanceof HttpContent) {
        	Root root = Root.getInstance();
            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
            	contentBuf.append(content.toString(CharsetUtil.UTF_8));
            }

            if (msg instanceof LastHttpContent) {
//                buf.append("END OF CONTENT\r\n");
                if (currentAction.equals("Add")) {
                	addMessageFromContent(contentBuf.toString(),root);
                	currentAction = "None";
                }
                LastHttpContent trailer = (LastHttpContent) msg;

                if (!writeResponse(trailer, ctx)) {
                    // If keep-alive is off, close the connection once the content is fully written.
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    private void addMessageFromContent(String msgContent, Root root) {
        //{"author":"Iron Man", "text":"Hello Jarvis!"}
       // System.out.println("msgContent : "+msgContent);
        String text = getText(msgContent);
        String author  = getAuthor(msgContent);
        String thread  = null;
       // buf.append("author: ").append(author).append("\r\n");
       // buf.append("text: ").append(text).append("\r\n");
        if (isThreadPresent(msgContent)) {
        	thread  = getThread(msgContent);
        //	buf.append("thread: ").append(thread).append("\r\n\r\n");
        	root.addMessage(author,text,thread);
        }
		
	}

	private String getAuthor(String msgContent) {
		//{"author":"Iron Man", "text":"Hello Jarvis!"}
		int indexBegin = msgContent.indexOf("author")+ 9;
		int indexEnd = msgContent.indexOf("\"", indexBegin);
		return msgContent.substring(indexBegin, indexEnd);
	}

	private String getText(String msgContent) {
		//{"author":"Iron Man", "text":"Hello Jarvis!"}
		int indexBegin = msgContent.indexOf("text")+ 7;
		int indexEnd = msgContent.indexOf("\"", indexBegin);
		return msgContent.substring(indexBegin, indexEnd);
	}

	private boolean isThreadPresent(String msgContent) {
		
		return msgContent.indexOf("thread") > 0;
		
	}	
	
	private String getThread(String msgContent) {
		//{"author":"Iron Man", "text":"Hello Jarvis!"}
		int indexBegin = msgContent.indexOf("thread")+ 8;
		int indexEnd = msgContent.indexOf("}", indexBegin);
		return msgContent.substring(indexBegin, indexEnd);
	}	


    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        // Build the response object.
        FullHttpResponse response;
        if (currentAction.equals("Add"))
        	response = new DefaultFullHttpResponse(
                    HTTP_1_1, currentObj.decoderResult().isSuccess()? CREATED : BAD_REQUEST);
        else if (currentAction.equals("Latest")) {
        	response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.decoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
        	response.headers().set(CONTENT_TYPE,"application/json; charset=utf-8");
        } else
        	response = new DefaultFullHttpResponse(
                    HTTP_1_1, currentObj.decoderResult().isSuccess()? OK : BAD_REQUEST,
                    Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

       // response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
        
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    private static String decode(String s) {
        if (s == null)
            return s;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuffer sb = new StringBuffer(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n;) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            int ui = i;
            for (;;) {
                assert (n - i >= 2);
                bb.put(decode(s.charAt(++i), s.charAt(++i)));
                if (++i >= n)
                    break;
                c = s.charAt(i);
                if (c != '%')
                    break;
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }
    
    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte)(  ((decode(c1) & 0xf) << 4)
                      | ((decode(c2) & 0xf) << 0));
    }
}
