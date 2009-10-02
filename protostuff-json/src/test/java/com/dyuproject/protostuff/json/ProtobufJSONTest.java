//========================================================================
//Copyright 2007-2008 David Yu dyuproject@gmail.com
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package com.dyuproject.protostuff.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.dyuproject.protostuff.model.V22Lite;
import com.dyuproject.protostuff.model.V22Lite.Person;
import com.dyuproject.protostuff.model.V22Lite.Task;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLite.Builder;

/**
 * @author David Yu
 * @created Sep 30, 2009
 */

public class ProtobufJSONTest extends TestCase
{
    static final LiteJSON LITE = new LiteJSON(new Class[]{V22Lite.class});
    static final NumericLiteJSON NUM_LITE = new NumericLiteJSON(new Class[]{V22Lite.class});
    static final ProtobufJSON<ProtobufConvertor<MessageLite, Builder>> GENERATED = 
        V22LiteGenerated.getJSON();
    
    static final Task task = Task.newBuilder()
        .setId(1)
        .setName("task_name")
        .setDescription("task_description")
        .setStatus(Task.Status.COMPLETED)
        .build();
    
    static final Person person = Person.newBuilder()
        .setId(1)
        .setFirstName("john")
        .setLastName("doe")
        .setAGe(2)
        .setEmail("john_doe@email.com")
        .setCurrentTask(task)
        //.addDelegatedTask(task)
        .addPriorityTask(task)
        .addPriorityTask(task)
        .addRepeatedLong(3)
        .addRepeatedLong(4)
        .addRepeatedLong(5)
        .build();
    
    static void doTestGenerateAndParse(PrintStream out) throws Exception
    {
        String generated = generateAndParse(GENERATED);
        String lite = generateAndParse(LITE);
        String numLite = generateAndParse(NUM_LITE);
        
        assertEquals(generated, lite);
        
        out.println(generated);
        out.println(lite);
        out.println(numLite);
    }
    
    static String generateAndParse(LiteJSON json) throws Exception
    {
        StringWriter sw = new StringWriter();
        json.writeMessage(sw, person);
        String generated = sw.toString();

        JsonParser parser = json.getJsonFactory().createJsonParser(generated);
        Person parsedPerson = json.readMessage(parser, Person.class);
        assertEquals(person, parsedPerson);
        parser.close();
        
        return generated;
    }
    
    static String generateAndParse(ProtobufJSON<ProtobufConvertor<MessageLite, Builder>> json) 
    throws Exception
    {
        StringWriter sw = new StringWriter();
        json.writeMessage(sw, person);
        String generated = sw.toString();

        JsonParser parser = json.getJsonFactory().createJsonParser(generated);
        Person parsedPerson = json.readMessage(parser, Person.class);
        assertEquals(person, parsedPerson);
        parser.close();
        
        return generated;
    }
    
    static void assertEquals(Person p, Person p2)
    {
        assertTrue(p.getId()==p2.getId());
        
        assertEquals(p.getFirstName(), p2.getFirstName());
        
        assertEquals(p.getLastName(), p2.getLastName());
        
        assertTrue(p.getAGe()==p2.getAGe());
        
        assertEquals(p.getEmail(), p2.getEmail());
        
        assertEquals(p.getCurrentTask(), p2.getCurrentTask());
        
        assertTrue(p.getDelegatedTaskCount()==p2.getDelegatedTaskCount());
        
        for(int i=0, len=p.getDelegatedTaskCount(); i<len; i++)
        {
            assertEquals(p.getDelegatedTask(i), p2.getDelegatedTask(i));
        }
    }
    
    static void assertEquals(Task t, Task t2)
    {
        
        if(t==null && t2==null)
            return;
        
        assertTrue(t.getId()==t2.getId());
        
        assertEquals(t.getName(), t2.getName());
        
        assertEquals(t.getDescription(), t2.getDescription());
        
        assertTrue(t.getStatus() == t2.getStatus());        
    }
    
    public void jacksonTest() throws Exception
    {
        JsonFactory factory = new JsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator generator = factory.createJsonGenerator(writer);
        generator.writeStartObject();
        generator.writeFieldName("bool");
        generator.writeBoolean(true);
        generator.writeFieldName("firstName");
        generator.writeString("john");
        generator.writeFieldName("age");
        generator.writeNumber(1);
        generator.writeFieldName("gg");
        generator.writeStartObject();
        generator.writeFieldName("firstName");
        generator.writeString("john");
        generator.writeEndObject();
        
        generator.writeEndObject();
        generator.close();
        String generated = writer.toString();
        System.out.print(generated);
        
        JsonParser parser = factory.createJsonParser(generated);
        assertTrue(parser.nextToken()==JsonToken.START_OBJECT);
        parser.nextToken();
        
        assertEquals("bool", parser.getCurrentName());
        assertTrue(parser.nextToken()==JsonToken.VALUE_TRUE);
        parser.nextToken();
        
        assertEquals("firstName", parser.getCurrentName());
        parser.nextToken();
        assertEquals("john", parser.getText());
        parser.nextToken();
        
        assertEquals("age", parser.getCurrentName());
        parser.nextToken();
        assertTrue(1==parser.getIntValue());
        parser.nextToken();
        
        assertEquals("gg", parser.getCurrentName());
        assertTrue(parser.nextToken()==JsonToken.START_OBJECT);
        parser.nextToken();
        assertEquals("firstName", parser.getCurrentName());
        parser.nextToken();
        assertEquals("john", parser.getText());
        assertTrue(parser.nextToken()==JsonToken.END_OBJECT);
        
        assertTrue(parser.nextToken()==JsonToken.END_OBJECT);
        
        parser.close();
    }
    
    public void testBenchmark() throws Exception
    {
        String dir = System.getProperty("benchmark.output_dir");
        
        PrintStream out = dir==null ? System.out : 
            new PrintStream(new FileOutputStream(new File(new File(dir), 
                    "protostuff-benchmark-"+System.currentTimeMillis()+".html"), true));
        
        int warmups = Integer.getInteger("benchmark.warmups", 100000);
        int loops = Integer.getInteger("benchmark.loops", 1000000);
        String title = "protostuff-json ser/deser benchmark for " + loops + " runs";
        out.println("<html><head><title>");
        out.println(title);
        out.println("</title></head><body><p>");
        out.println(title);        
        out.println("</p><pre>\n\n");
        
        doTestGenerateAndParse(out);
        out.println("\n\n</pre><hr/><pre>");
        start(out, warmups, loops);
        
        if(System.out!=out)
            out.close();
    }
    
    public static void main(String[] args) throws Exception
    {
        String dir = System.getProperty("benchmark.output_dir");
        
        PrintStream out = dir==null ? System.out : 
            new PrintStream(new FileOutputStream(new File(new File(dir), 
                    "protostuff-benchmark-"+System.currentTimeMillis()+".html"), true));
        int runs = Integer.getInteger("benchmark.runs", 1);
        
        
        int warmups = Integer.getInteger("benchmark.warmups", 100000);
        int loops = Integer.getInteger("benchmark.loops", 1000000);
        String title = "protostuff-json ser/deser benchmark for " + loops + " runs";
        out.println("<html><head><title>");
        out.println(title);
        out.println("</title></head><body><p>");
        out.println(title);
        out.println("</p><pre>\n\n");
        
        doTestGenerateAndParse(out);
        out.println("\n\n</pre><hr/><pre>");
        for(int i=0; i<runs; i++)
            start(out, warmups, loops);
        
        if(System.out!=out)
            out.close();
    }
    
    public static void start(PrintStream out, int warmups, int loops) throws Exception
    {
        Charset utf8 = Charset.forName("UTF-8");
        
        int generatedLen = generateAndParse(GENERATED).getBytes(utf8).length;
        for(int i=0; i<warmups; i++)
            generateAndParse(GENERATED);        
        long generatedStart = System.currentTimeMillis();
        for(int i=0; i<loops; i++)
            generateAndParse(GENERATED);
        long generatedFinish = System.currentTimeMillis();
        long generatedElapsed = generatedFinish - generatedStart;
        out.println(generatedElapsed + " ms elapsed with " + generatedLen + " bytes for GENERATED");
        
        int liteLen = generateAndParse(LITE).getBytes(utf8).length;
        for(int i=0; i<warmups; i++)
            generateAndParse(LITE);    
        long liteStart = System.currentTimeMillis();
        for(int i=0; i<loops; i++)
            generateAndParse(LITE);
        long liteFinish = System.currentTimeMillis();
        long liteElapsed = liteFinish - liteStart;
        out.println(liteElapsed + " ms elapsed with " + liteLen + " bytes for LITE");
        
        int numLiteLen = generateAndParse(NUM_LITE).getBytes(utf8).length;
        for(int i=0; i<warmups; i++)
            generateAndParse(NUM_LITE);
        long numLiteStart = System.currentTimeMillis();
        for(int i=0; i<loops; i++)
            generateAndParse(NUM_LITE);
        long numLiteFinish = System.currentTimeMillis();
        long numLiteElapsed = numLiteFinish - numLiteStart;
        out.println(numLiteElapsed + " ms elapsed with " + numLiteLen + " bytes for NUM_LITE");
        
        

    }

}
