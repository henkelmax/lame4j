# Lame4J

A Java wrapper for [LAME](https://lame.sourceforge.io/index.php).

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.lame4j</groupId>
  <artifactId>lame4j</artifactId>
  <version>1.0.0</version>
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
  implementation 'de.maxhenkel.lame4j:lame4j:1.0.0'
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
LameDecoder decoder = new LameDecoder(Files.newInputStream(Paths.get("myfile.mp3")));
short[] decode = decoder.decode();

System.out.println("Sample Rate: " + decoder.getSampleRate());
System.out.println("Bitrate: " + decoder.getBitrate());
System.out.println("Channels: " + decoder.getChannelCount());
System.out.println("FrameSize: " + decoder.getFrameSize());
System.out.println("Length: " + decode.length + " samples");
System.out.println("Duration: " + ((float) decode.length / (float) decoder.getSampleRate()) + " seconds");

LameEncoder encoder = new LameEncoder(decoder.getChannelCount(), decoder.getSampleRate(), decoder.getBitrate(), 5, Files.newOutputStream(Paths.get("mynewfile.mp3")));
encoder.write(decode);
encoder.close();
```


## Credits

- [LAME website](https://lame.sourceforge.io/)
- [License](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/COPYING)
- [Windows binaries](https://www.rarewares.org/mp3-lame-libraries.php)
- [MacOS binaries](https://www.rarewares.org/mp3-lame-bundle.php)
- [Linux binaries](https://packages.debian.org/buster/libmp3lame0)

<details>
  <summary>Other stuff</summary>

- [API](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/API)
- [Headers](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/include/lame.h)
- [Docs.rs](https://docs.rs/lame-sys/0.1.2/lame_sys/)

</details>