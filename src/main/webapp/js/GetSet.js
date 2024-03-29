
function submitForm() {
    document.getElementById("myForm").submit();
}
document.addEventListener('DOMContentLoaded', function () {
    var flashcards = document.getElementsByClassName('flashcard');
    var currentIndex = 0;

    function showFlashcard(index) {
        if (index >= 0 && index < flashcards.length) {
            for (var i = 0; i < flashcards.length; i++) {
                flashcards[i].classList.add('hidden');
            }
            flashcards[index].classList.remove('hidden');
            currentIndex = index;
        }
    }

    function showNextFlashcard() {
        var nextIndex = currentIndex + 1;
        if (nextIndex >= flashcards.length) {
            nextIndex = 0;
        }
        showFlashcard(nextIndex);
    }

    function showPreviousFlashcard() {
        var prevIndex = currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = flashcards.length - 1;
        }
        showFlashcard(prevIndex);
    }

    for (var i = 0; i < flashcards.length; i++) {
        flashcards[i].addEventListener('click', function () {
            this.classList.toggle("flipped");
        });
    }

    var nextBtn = document.getElementById('nextBtn');
    var prevBtn = document.getElementById('prevBtn');

    nextBtn.addEventListener('click', showNextFlashcard);
    prevBtn.addEventListener('click', showPreviousFlashcard);

    showFlashcard(currentIndex);
});

// js for comments
function toggleForm(replyId) {
    var divContainsForm = document.getElementById('commentId=' + replyId);
    if (divContainsForm.style.display === 'none' || form.style.display === '') {
        divContainsForm.style.display = 'block';
    } else {
        divContainsForm.style.display = 'none';
    }
}



