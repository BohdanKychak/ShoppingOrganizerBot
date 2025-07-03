#Shopping Organizer Bot

**@ShoppingOrganizerBot** is a Telegram bot designed for convenient shopping and personal finance management.
It allows you to store, edit, and organize shopping lists in an easy-to-understand format.
It has tools for budget control, accounting of expenses and income.
Supports multiple currencies with automatic exchange rate updates for accurate financial calculations.

The bot works in two modes:
- Webhook mode for constant work on the server
- Long Polling for local testing

## ⚙️ Current status

- 🌐 Deployed on [Oracle Cloud Free Tier] (https://www.oracle.com/cloud/)
- 📦 Written in **Java** using **Spring Boot**.
- 💾 Data is stored in **PostgreSQL** database
- 🛡️ Configured CI/CD via **GitHub Actions**.
- 🔐 Secrets and deployments are performed via SSH key

## 📁 Project structure

- `src/main/java` - the main logic of the bot
- `src/main/resources` - configuration and localization files
- `.github/workflows/deploy.yml` - automatic update after pushing to `master`

---

> Attention: the project is under active development.
