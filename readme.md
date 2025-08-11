# Lame4J

A Java wrapper for [LAME](https://lame.sourceforge.io/index.php) written in C using JNI.
This also includes [minimp3](https://github.com/lieff/minimp3) to decode mp3 files.

Java 8+ is required to use this library.

## Supported Platforms

- `Windows x86_64`
- `Windows aarch64`
- `macOS x86_64`
- `macOS aarch64`
- `Linux x86_64`
- `Linux aarch64`

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.lame4j</groupId>
  <artifactId>lame4j</artifactId>
  <version>2.1.0</version>
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
  implementation 'de.maxhenkel.lame4j:lame4j:2.1.0'
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

## Building from Source

### Prerequisites

- [Java](https://www.java.com/en/) 21
- [Zig](https://ziglang.org/) 0.14.1
- [Ninja](https://ninja-build.org/)

### Building

``` bash
./gradlew build
```

## Credits

- [LAME](https://lame.sourceforge.io/)
- [LAME License](https://sourceforge.net/p/lame/svn/HEAD/tree/tags/RELEASE__3_100/lame/COPYING)
- [minimp3](https://github.com/lieff/minimp3)
