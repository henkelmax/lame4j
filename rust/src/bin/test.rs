use std::fs::File;
use std::io;
use std::io::Read;
use std::path::Path;
use std::time::Duration;
use minimp3::{Decoder, Frame, Error};

fn main() {
    // let mut decoder = Decoder::new(File::open("E:\\Workspaces\\lame4j\\target\\doc_goathorn.mp3").unwrap());
    let mut decoder = Decoder::new(File::open("E:\\Workspaces\\lame4j\\target\\bassglide.mp3").unwrap());

    loop {
        match decoder.next_frame() {
            Ok(Frame { data, sample_rate, channels, .. }) => {
                println!("Decoded {} samples", data.len() / channels)
            },
            Err(Error::Eof) => break,
            Err(e) => panic!("{:?}", e),
        }
    }
}