
# 🚗​ PotHoles
## 🤔​ What is PotHoles? 
PotHoles is a project assigned during Laboratory of Operating Systems course at Università di Napoli Federico II.
PotHoles is a client-server system designed to manage pothole detection.
This project is divided into a client (an Android app) and a server, written in _C_ and running in a UNIX environment.
### ​📱​ Client
As mentioned before, the client is an android app written in Java (as specifically required).\
the user can start a recording session where the app detects potholes using the smartphone’s accelerometer along the y-axis.\
If the accelerometer exceeds a certain threshold, a new pothole is detected.
The user can also check whether there are new events (potholes) within a specific range.
### ​💻​ Server
The server is written in C language and was hosted on _Microsoft Azure_ (as specifically required).\
A simple communication protocol called **ECM** (_Enhanced Coordinate Messaging Protocol_)Its structure is as follows:
<p align="center"> HEADER -> PACKET_SIZE -> COMMAND -> OTHER_DATA_ACCORDING_TO_THE_COMMAND </p>

The server is a multi-thred where instead of assign a new connection to a new thread, a thread pool has been developed; this choice was made to design something closer to real-world servers, rather than a purely academic solution. 
For data managment was used a simple databse in **PostegreSQL**.


For more detaled info you can check the [documentation](LSO_2122_Doc.pdf)

## 🚀​ Server Installation

The server needs to be used in a UNIX enviroment.\
To compile it:
```bash
  gcc -Wall -Wextra -g -pthread -o main.run main.c ecmprotocol.c ecmdb.c threadpool.c -I/usr/include/postgresql/ -L/usr/lib/postgresql/14/lib/ -lpq
```
To start the program:
```bash
  ./main.run [argument] <option> ...
```
where the possible arguments are:
- -t: thread number assigned to the pool (default is 4)
- -q: queue length for the thread pool (default is 20)
- -a: threshold value for the accelerometer (default is 10.0)
An possible run could be:
```bash
  ./main.run -t 4 -q 20 -a 10,0
```
or
```bash
  ./main.run -t 10 -q 30 -a 5,0
```

## ​🖼️​ Screenshots 
- In the following screen you can see a recording session while a new pothole has been found. As you can see there is also a red mark on the map showing danger so every user within a specific range can see that.
- ![Screenshot 2025-04-13 212227](https://github.com/user-attachments/assets/5a8759ed-1ae3-42c3-acc7-ae061e7b9d32)
- In the following screen you can see a list of potholes found in within a specific range.
- ![Screenshot 2025-04-13 212237](https://github.com/user-attachments/assets/38e0e0e6-d8f5-4035-9763-feb46a191f70)

## ⛏️​ Logo
<img src="https://github.com/user-attachments/assets/9a2f23cb-e8e4-4253-af4e-7f268c8c366b" width="200" align="center">

## ​🧑🏻‍💻​ Authors
This project was made in collaboration with:
- https://github.com/matthyzza
- https://github.com/emanuele989

## 📫​ Support
For any questions feel free to reach me anytime; my contacts are available in my homepage.

