package com.example.JournalApp.controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.JournalApp.Entity.JournalEntity;

@RestController
@RequestMapping("/journal")
public class JounralEntryController {
    private Map<Long, JournalEntity> journalEntries = new HashMap<>();

    @GetMapping
    public List<JournalEntity> getAll(){
        return new ArrayList<>(journalEntries.values());
    }

    @PostMapping
    public boolean createEntry(@RequestBody JournalEntity myEntry){
        journalEntries.put(myEntry.getId(), myEntry);
        return true;
    }

    @GetMapping("id/{id}")
    public JournalEntity getJournalEntryById(@PathVariable Long id){
        return journalEntries.get(id);
    }

    @DeleteMapping("id/{id}")
    public JournalEntity deleteJournalEntryById(@PathVariable Long id) {
        return journalEntries.remove(id);
    }

    @PutMapping("id/{id}")
    public JournalEntity updateJournalEntry(@PathVariable Long id, @RequestBody JournalEntity myEntry){
        return journalEntries.put(id, myEntry);
    }
}
