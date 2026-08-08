package com.quizapp.service;

import com.quizapp.dto.QuestionLabelDto;
import com.quizapp.dto.QuestionLabelRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.QuestionLabel;
import com.quizapp.repository.QuestionLabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionLabelService {

    private final QuestionLabelRepository labelRepository;

    public QuestionLabelService(QuestionLabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionLabelDto> findAll() {
        return labelRepository.findAllByOrderByNameAsc().stream()
                .map(l -> new QuestionLabelDto(l.getId(), l.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionLabelDto create(QuestionLabelRequest request) {
        String name = request.getName().trim();
        if (labelRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A label named '" + name + "' already exists.");
        }
        QuestionLabel label = new QuestionLabel();
        label.setName(name);
        QuestionLabel saved = labelRepository.save(label);
        return new QuestionLabelDto(saved.getId(), saved.getName());
    }

    // Renaming needs no cascade at all - unlike GridCategory (which stores the
    // category name directly on athletes/grids/etc.), labels are a real
    // foreign-key relationship, so every question already linked to this
    // label picks up the new name automatically.
    @Transactional
    public QuestionLabelDto update(Long id, QuestionLabelRequest request) {
        QuestionLabel label = labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No label found with id " + id));
        String newName = request.getName().trim();
        if (!label.getName().equalsIgnoreCase(newName) && labelRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("A label named '" + newName + "' already exists.");
        }
        label.setName(newName);
        labelRepository.save(label);
        return new QuestionLabelDto(label.getId(), label.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new ResourceNotFoundException("No label found with id " + id);
        }
        // No delete-blocking needed - removing a label from a question it was
        // attached to is a harmless, minor change (unlike deleting an
        // athlete's grid data), so deleting the label itself here is fine
        // even if some questions still reference it; the join-table rows
        // just get cleaned up along with it.
        labelRepository.deleteById(id);
    }
}
