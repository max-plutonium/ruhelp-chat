package com.pirotehnika_ruhelp.chat

case class Message(id: String, name: String, timestamp: String, text: String)

case class Member(name: String, href: String, lastTime: String)