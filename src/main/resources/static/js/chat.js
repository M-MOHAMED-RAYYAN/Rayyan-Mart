/* RayyanMart shopping assistant widget.
 * Talks only to the same-origin /api/chat endpoint. The browser never reads or
 * stores any API key. Replies are inserted as text (never HTML) to prevent XSS.
 */
(function () {
  "use strict";

  var toggle = document.getElementById("rm-chat-toggle");
  var panel = document.getElementById("rm-chat-panel");
  var closeBtn = document.getElementById("rm-chat-close");
  var form = document.getElementById("rm-chat-form");
  var input = document.getElementById("rm-chat-text");
  var messages = document.getElementById("rm-chat-messages");
  var chips = document.getElementById("rm-chat-chips");

  if (!toggle || !panel) {
    return;
  }

  var maxLength = 500;
  var pending = false;
  var typing = document.getElementById("rm-chat-typing");

  function setState(open) {
    toggle.classList.toggle("open", open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
    toggle.setAttribute("aria-label", open ? "Close the RayyanMart assistant" : "Open the RayyanMart assistant");
    var icon = document.getElementById("rm-chat-toggle-icon");
    if (icon) {
      icon.textContent = open ? "\u2715" : "?";
    }
  }

  function open() {
    panel.hidden = false;
    setState(true);
    if (input) {
      input.focus();
    }
  }

  function close() {
    panel.hidden = true;
    setState(false);
  }

  function appendMessage(text, who) {
    var bubble = document.createElement("div");
    bubble.className = "chat-msg " + (who === "user" ? "user" : "bot");
    bubble.textContent = text;
    messages.appendChild(bubble);
    messages.scrollTop = messages.scrollHeight;
  }

  function disabled(flag) {
    if (input) {
      input.disabled = flag;
    }
    var submit = form.querySelector('button[type="submit"]');
    if (submit) {
      submit.disabled = flag;
    }
  }

  function setTyping(on) {
    if (typing) {
      typing.hidden = !on;
      if (on && messages) {
        messages.scrollTop = messages.scrollHeight;
      }
    }
  }

  function appendErrorWithRetry(question) {
    var bubble = document.createElement("div");
    bubble.className = "chat-msg bot error";
    bubble.textContent = "I could not reach the assistant. Please check your connection and try again.";
    messages.appendChild(bubble);
    var retry = document.createElement("button");
    retry.type = "button";
    retry.className = "chat-retry";
    retry.textContent = "Retry";
    retry.addEventListener("click", function () {
      retry.remove();
      send(question);
    });
    messages.appendChild(retry);
    messages.scrollTop = messages.scrollHeight;
  }

  function send(question) {
    if (pending) {
      return;
    }
    question = (question || "").trim();
    if (!question) {
      return;
    }
    if (question.length > maxLength) {
      appendMessage("That message is too long. Please keep it under " + maxLength + " characters.", "bot");
      return;
    }
    appendMessage(question, "user");
    if (input) {
      input.value = "";
    }
    disabled(true);
    pending = true;
    setTyping(true);

    fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: question })
    })
      .then(function (res) {
        if (res.status === 429) {
          appendMessage("You are sending messages too quickly. Please wait a moment before continuing.", "bot");
          return null;
        }
        if (res.status === 400) {
          return res.json().then(function (body) {
            appendMessage(body && body.message ? body.message : "Please rephrase your question.", "bot");
            return null;
          });
        }
        if (!res.ok) {
          return Promise.reject(new Error("chat unavailable"));
        }
        return res.json();
      })
      .then(function (data) {
        if (data && data.reply) {
          appendMessage(data.reply, "bot");
        }
      })
      .catch(function () {
        appendErrorWithRetry(question);
      })
      .then(function () {
        disabled(false);
        pending = false;
        setTyping(false);
        if (input) {
          input.focus();
        }
      });
  }

  toggle.addEventListener("click", function () {
    if (panel.hidden) {
      open();
    } else {
      close();
    }
  });
  closeBtn.addEventListener("click", close);

  form.addEventListener("submit", function (ev) {
    ev.preventDefault();
    send(input ? input.value : "");
  });

  if (chips) {
    chips.addEventListener("click", function (ev) {
      var chip = ev.target.closest(".chat-chip");
      if (chip && chip.getAttribute("data-q")) {
        send(chip.getAttribute("data-q"));
      }
    });
  }
})();