
Make null-safe collections in thrift generated java object.


```java
foo.read(tProtocol);  // read thrift object
assertThat(foo.getList(), is(nullValue()));
assertThat(foo.getBar().getList(), is(nullValue()));

foo = w(foo);  // call ThriftWrapper.w()
assertThat(foo.getList(), is(emptyCollectionOf(String.class)));
assertThat(foo.getBar().getList(), is(emptyCollectionOf(String.class)));

foo.write(tProtocol);  // doesn't send collections if they were initially null and still empty
```

# motivation

While I was experimenting [evernote java API](http://dev.evernote.com/doc/), I found thrift generated code for container types(list,set,map) are not really java friendly, especially for for-loop. In thrift, when value is set to null, the transport will not send the field. But in java, when collection is null, the enhanced-for-loop throws NullPointerException.

# mechanism

The wrapping method introspect the given thrift object and set empty mutable collections(ArrayList, HashSet, HashMap) when it finds null in collection fields. Also, it traverse child thrift classes and set empty collections as well.(foo.bar.baz.list).
Then it returns cglib generated proxy. Since in thrift, null has a meaning when it serialize object, the proxy keeps track of collection fields which were initially null, and when "write()" method is called, it compare the current value and if it's still empty, then it will omit the field to transport.

# further

Please see [test classes](https://github.com/ttddyy/nullsafe-thrift/tree/master/src/test/java/net/ttddyy/nullsafethrift) for more detailed behavior.

Since I'm new to thrift, it might not be a good approach or there might be a better solution already.
If there is more request for this approach, I'll further enhance the project such:

- inline cglib to avoid dependency conflict
- make the class more spring-framework friendly bean instead of static methods
- release and push to public maven repo
- etc.


# links

[my blog post](http://tadtech.blogspot.com/2013/12/library-to-make-null-safe-collections.html)


