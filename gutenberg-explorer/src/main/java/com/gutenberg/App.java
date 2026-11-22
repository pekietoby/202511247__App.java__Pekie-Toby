package com.gutenberg;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Scanner;

public class App {
    static final int MAX_BOOKS = 25;

    static class Book {
        int id;
        String title;
        String author;
        String url;

        Book(int id, String title, String author, String url) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.url = url;
        }

        @Override
        public String toString() {
            return "[" + id + "] " + title + " — " + author;
        }
    }

    // Fixed-size array (capacity 25 > 20 minimum, no ArrayList)
    static Book[] catalog = new Book[MAX_BOOKS];
    static int bookCount = 0;

    public static void main(String[] args) {
        loadPopularBooks();
        System.out.println("Loaded " + bookCount + " books into fixed array (capacity: " + MAX_BOOKS + ").");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n--- Project Gutenberg Book Explorer ---");
            System.out.println("1. List all loaded books");
            System.out.println("2. Search by title/keyword");
            System.out.println("3. Search by author");
            System.out.println("4. Search by book ID");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    printBooks(searchBook());
                    break;
                case "2":
                    System.out.print("Enter title keyword: ");
                    String keyword = scanner.nextLine().trim();
                    printBooks(searchBook(keyword));
                    break;
                case "3":
                    System.out.print("Enter author name: ");
                    String author = scanner.nextLine().trim();
                    printBooks(searchBook(author, true));
                    break;
                case "4":
                    System.out.print("Enter Gutenberg book ID: ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        Book found = searchBook(id);
                        if (found != null) {
                            System.out.println("Found: " + found + " (" + found.url + ")");
                        } else {
                            System.out.println("No book found with ID " + id + ".");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid numeric ID.");
                    }
                    break;
                case "5":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        scanner.close();
    }

    static void loadPopularBooks() {
        bookCount = 0;
        String url = "https://www.gutenberg.org/ebooks/search/?sort_order=downloads";
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("li.booklink");
            for (Element item : items) {
                if (bookCount >= MAX_BOOKS) {
                    break;
                }

                Element link = item.selectFirst("a.link");
                if (link == null) {
                    continue;
                }

                String href = link.attr("href");
                int id = extractId(href);
                if (id <= 0) {
                    continue;
                }

                Element titleEl = item.selectFirst("span.title");
                Element authorEl = item.selectFirst("span.subtitle");

                String title = (titleEl != null && !titleEl.text().isBlank())
                        ? titleEl.text().trim()
                        : link.text().trim();
                String author = (authorEl != null && !authorEl.text().isBlank())
                        ? authorEl.text().trim()
                        : "Unknown Author";
                String fullUrl = "https://www.gutenberg.org" + href;

                catalog[bookCount] = new Book(id, title, author, fullUrl);
                bookCount++;
            }
        } catch (IOException e) {
            System.out.println("Could not fetch live Gutenberg page (" + e.getMessage() + "). Using starter classics...");
        }

        // Fallback entries if offline or Gutenberg changes markup
        if (bookCount == 0) {
            addFallbackBook(1342, "Pride and Prejudice", "Jane Austen");
            addFallbackBook(84, "Frankenstein", "Mary Shelley");
            addFallbackBook(11, "Alice's Adventures in Wonderland", "Lewis Carroll");
            addFallbackBook(1661, "The Adventures of Sherlock Holmes", "Arthur Conan Doyle");
            addFallbackBook(2701, "Moby-Dick", "Herman Melville");
        }
    }

    static void addFallbackBook(int id, String title, String author) {
        if (bookCount < MAX_BOOKS) {
            catalog[bookCount] = new Book(id, title, author, "https://www.gutenberg.org/ebooks/" + id);
            bookCount++;
        }
    }

    static int extractId(String href) {
        if (href == null) {
            return -1;
        }
        String[] parts = href.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isBlank() && parts[i].matches("\\d+")) {
                return Integer.parseInt(parts[i]);
            }
        }
        return -1;
    }

    // Overloaded searchBook #1: returns all active books in a fixed-size array
    static Book[] searchBook() {
        Book[] results = new Book[bookCount];
        for (int i = 0; i < bookCount; i++) {
            results[i] = catalog[i];
        }
        return results;
    }

    // Overloaded searchBook #2: search by title keyword
    static Book[] searchBook(String titleKeyword) {
        Book[] temp = new Book[bookCount];
        int matchCount = 0;
        String target = titleKeyword.toLowerCase();

        for (int i = 0; i < bookCount; i++) {
            if (catalog[i] != null && catalog[i].title.toLowerCase().contains(target)) {
                temp[matchCount] = catalog[i];
                matchCount++;
            }
        }

        Book[] matches = new Book[matchCount];
        for (int i = 0; i < matchCount; i++) {
            matches[i] = temp[i];
        }
        return matches;
    }

    // Overloaded searchBook #3: search by author name (boolean flag distinguishes signature)
    static Book[] searchBook(String authorQuery, boolean byAuthor) {
        if (!byAuthor) {
            return searchBook(authorQuery);
        }
        Book[] temp = new Book[bookCount];
        int matchCount = 0;
        String target = authorQuery.toLowerCase();

        for (int i = 0; i < bookCount; i++) {
            if (catalog[i] != null && catalog[i].author.toLowerCase().contains(target)) {
                temp[matchCount] = catalog[i];
                matchCount++;
            }
        }

        Book[] matches = new Book[matchCount];
        for (int i = 0; i < matchCount; i++) {
            matches[i] = temp[i];
        }
        return matches;
    }

    // Overloaded searchBook #4: search by Gutenberg book ID
    static Book searchBook(int bookId) {
        for (int i = 0; i < bookCount; i++) {
            if (catalog[i] != null && catalog[i].id == bookId) {
                return catalog[i];
            }
        }
        return null;
    }

    static void printBooks(Book[] books) {
        if (books == null || books.length == 0) {
            System.out.println("No matching books found.");
            return;
        }
        for (int i = 0; i < books.length; i++) {
            System.out.println((i + 1) + ". " + books[i]);
        }
    }
}