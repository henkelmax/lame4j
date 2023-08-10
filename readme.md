# Lame4J

A Java wrapper for [LAME](https://lame.sourceforge.io/index.php) written in Rust using JNI.
This also includes [minimp3](https://github.com/lieff/minimp3) to decode mp3 files.

This library includes natives for:

- `Windows x86`
- `Windows x64`
- `MacOS x64`
- `MacOS aarch64`
- `Linux x86`
- `Linux x64`
- `Linux aarch64`

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.lame4j</groupId>
  <artifactId>lame4j</artifactId>
  <version>2.0.3</version>
</dependency>

<repositories>
  <repository>
    <id>henkelmax.public</id>
    <url>https://maven.maxhenkel.de/repository/public</url>
  </repository>
</repositories>
```

**Gradle**

``` groovy
dependencies {
  implementation 'de.maxhenkel.lame4j:lame4j:2.0.3'
}

repositories {
  maven {
    name = "henkelmax.public"
    url = 'https://maven.maxhenkel.de/repository/public'
  }
}
```

## Example

``` java
DecodedAudio decodedAudio = Mp3Decoder.decode(Files.newInputStream(Paths.get("myfile.mp3")));

short[] decode = decodedAudio.getSamples();

System.out.println("Sample Rate: " + decodedAudio.getSampleRate());
System.out.println("Bit Rate: " + decodedAudio.getBitRate());
System.out.println("Channels: " + decodedAudio.getChannelCount());
System.out.println("Frame Size: " + decodedAudio.getSampleSizeInBits());

System.out.println("Length: " + decode.length + " samples");
System.out.println("Duration: " + ((float) decode.length / (float) decodedAudio.getSampleRate()) + " seconds");

Mp3Encoder encoder = new Mp3Encoder(decodedAudio.getChannelCount(), decodedAudio.getSampleRate(), decodedAudio.getBitRate(), 5, Files.newOutputStream(Paths.get(args[1])));
encoder.write(decode);
encoder.close();
```


## Credits

- [LAME](https://lame.sourceforge.io/)
- [LAME License](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/COPYING)
- [mp3lame-sys](https://github.com/DoumanAsh/mp3lame-sys)
- [minimp3-rs](https://github.com/germangb/minimp3-rs)
- [minimp3](https://github.com/lieff/minimp3)
- [jni-rs](https://github.com/jni-rs/jni-rs)

<details>
  <summary>Other stuff</summary>

- [API](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/API)
- [Headers](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/include/lame.h)
- [Docs.rs](https://docs.rs/lame-sys/0.1.2/lame_sys/)

</details>