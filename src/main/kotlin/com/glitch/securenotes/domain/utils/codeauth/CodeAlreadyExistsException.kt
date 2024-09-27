package com.glitch.securenotes.domain.utils.codeauth

class CodeAlreadyExistsException: Throwable("This auth code is already in use")